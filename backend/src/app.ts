import Fastify from 'fastify';
import { z } from 'zod';
import Redis from 'ioredis';
import { execFile } from 'child_process';
import util from 'util';

const execFileAsync = util.promisify(execFile);
const fastify = Fastify({ logger: true });

// Read environment values gracefully
const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';
const redisClient = process.env.NODE_ENV === 'test' ? null : new Redis(REDIS_URL);

// Standard schema validation using Zod
const MediaLookupSchema = z.object({
  url: z.string().url().refine((val) => {
    return /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be|instagram\.com|tiktok\.com)\/.*$/.test(val);
  }, { message: "Security Violation: Source URL belongs to an unsupported provider." })
});

// Primary extraction endpoint wrapping yt-dlp server-side
fastify.post('/v1/media/info', async (request, reply) => {
  try {
    const parseResult = MediaLookupSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({ error: "BAD_REQUEST", details: parseResult.error.format() });
    }

    const targetUrl = parseResult.data.url;
    const cacheKey = `vidora:cache_url:${Buffer.from(targetUrl).toString('base64')}`;

    // Redis optimization lookup
    if (redisClient) {
      const cached = await redisClient.get(cacheKey);
      if (cached) {
        return reply.status(200).send(JSON.parse(cached));
      }
    }

    // Call local system yt-dlp binary
    const { stdout } = await execFileAsync('yt-dlp', [
      '--dump-json',
      '--no-playlist',
      '--skip-download',
      '--no-warnings',
      targetUrl
    ], { timeout: 15000, maxBuffer: 10 * 1024 * 1024 });

    const rawParsed = JSON.parse(stdout);

    const resultPayload = {
      title: rawParsed.title,
      duration: rawParsed.duration || 0,
      channel: rawParsed.uploader || "Unknown Creator",
      thumbnail: rawParsed.thumbnail || "",
      formats: (rawParsed.formats || [])
        .filter((f: any) => f.vcodec !== 'none' || f.acodec !== 'none')
        .map((f: any) => ({
          format_id: f.format_id,
          url: f.url || null,
          ext: f.ext,
          quality_label: f.format_note || `${f.width || 0}p`,
          width: f.width || null,
          height: f.height || null,
          approx_bytes: f.filesize || f.filesize_approx || null
        }))
    };

    if (redisClient) {
      await redisClient.setex(cacheKey, 43200, JSON.stringify(resultPayload)); // 12 hours TTL
    }

    return reply.status(200).send(resultPayload);
  } catch (error: any) {
    fastify.log.error(error);
    return reply.status(500).send({ error: "EXTRACTION_FAILURE", msg: error.message });
  }
});

// App Health Check interface
fastify.get('/v1/health', async (request, reply) => {
  return reply.status(200).send({
    status: "healthy",
    version: "2.0.0",
    engine: "yt-dlp auto-update pipeline active"
  });
});

const startServer = async () => {
  try {
    const port = parseInt(process.env.PORT || '8080');
    await fastify.listen({ port, host: '0.0.0.0' });
    console.log(`Backend server listing on port: ${port}`);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
};

if (process.env.NODE_ENV !== 'test') {
  startServer();
}

export default fastify;
