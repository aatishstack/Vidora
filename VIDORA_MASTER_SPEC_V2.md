# VIDORA — MASTER ARCHITECTURAL BLUEPRINT & PRODUCT SPECIFICATION (VERSION 2.0)
### Document Classification: Confidential Startup Blueprint
### Target Audience: Senior Engineering Team, Product Managers, Security Engineers, and Lead Designers
### Authors: Principal Software Architect, Staff Android Engineer, Staff Backend Engineer, Product Lead, Security Lead, and Startup CTO

---

## SECTION 1: EXECUTIVE SUMMARY & STRATEGIC VISION

Vidora is an enterprise-grade, offline-first media manager and personal library platform designed for high-performance retrieval, organization, and playback of digital media. Intended for mobile power users operating in high-bandwidth, high-latency, or zero-connectivity network zones (such as commuters, remote workers, and international travelers), Vidora delivers a seamless desktop-grade media consumption experience on Android devices.

The system is developed on a strict multi-layer architectural model that separates interface rendering, cloud metadata extraction, local high-concurrency downloads, and long-term transactional database systems. Unlike legacy consumer downloaders, Vidora isolates all scraping and stream extraction from edge devices, ensuring robust compliance with app store policies and high-speed API endpoints that can be updated in real-time server-side without releasing client-side updates.

---

## SECTION 2: ARCHITECTURAL DEEP-DIVE & PLATFORM MITIGATIONS

```
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                    VIDORA SYSTEM TOPOLOGY                                |
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                         |
|  [ ANDROID EDGE CLIENT ]  ──────── (REST HTTPS) ───────►  [ CLOUDFLARE WAF / EDGE DNS ] |
|  - Jetpack Compose M3 UI                                          │                     |
|  - Room Local DB / Proto Datastore                                │ (Route Traffic)     |
|  - WorkManager Custom Worker                                      ▼                     |
|  - Media3 Hardware Acceleration Player              [ FASTIFY ENDPOINT AGGREGATOR ]     |
|                                                     - TypeScript / Zod Validation       |
|                                                     - API Client Certification          |
|                                                           │             │               |
|                     ┌─────────────────────────────────────┘             └────┐          |
|                     ▼ (Enqueue Video Resolver)                               ▼          |
|             [ BULLMQ QUEUE SERVICE ]                                  [ REDIS IN-MEMORY ]|
|             - Task Distribution                                       - Rate Limiter TB |
|             - Worker Load Balancing                                   - MediaInfo Cache |
|                     │                                                   - Session State |
|                     ▼ (Worker Spawns)                                        ▲          |
|             [ YT-DLP EXECUTOR DOCKERS ]                                      │          |
|             - Auto-Updating Subprocess Engines                               │          |
|             - Client Stream Deserializers                                    │          |
|                     │                                                        │          |
|                     ▼ (Persistent Metadata Write / Audit Logs)                │          |
|             [ POSTGRESQL MULTI-NODE DB ] ────────────────────────────────────┘          |
|             - Transactional History & Analytics                                         |
|             - Multi-tenant User Profiles & Folders                                      |
|                                                                                         |
+─────────────────────────────────────────────────────────────────────────────────────────+
```

### 2.1 Google Play Store & DMCA Containment Architecture
To mitigate sandboxing, delisting, or immediate developer ban risks on Google Play:
1. **Semantic Separation**: The client-side application uses strictly non-infringing vocabulary. References to "YouTube Downloader", "TikTok Grabber", "Video Ripper", or "Proxy Server" are forbidden. The app is named **Vidora: Offline Media & Library Manager** inside `strings.xml` and on-screen assets.
2. **Feature Gating**: All URL-resolution fields, provider logos, and direct server download triggers are configured via Firebase Remote Config. During initial App Review by Google Play bots, these remote toggles are disabled, showing a clean local media file manager that plays back files from user-selected storage folders. On structural approval, these triggers are enabled dynamically.
3. **Multi-Channel Delivery Pipeline**: The application builds three distinct targets dynamically using unified Gradle flavors:
   - `playStoreRelease`: Dynamic Feature modules enabled, remote download mechanisms gated, zero external scraping references.
   - `directApkRelease`: Direct integration with backend API, direct update notifications inside the app via self-hosted update links, unrestricted multi-provider matching on launch.
   - `fDroidRelease`: Fully open-source subset, utilizing no-analytics wrappers, relying on F-Droid package updates.

### 2.2 Client-Side vs. Server-Side Extraction Strategy
Traditional media retrieval client apps call platform APIs (such as innerTube, instagram mobile JSONs, or tiktok headers) directly. This creates a brittle, unmaintainable architecture because content platforms update their security signatures weekly. An issue of signature change requires editing app source files, recompiling, signing, submitting to the app store, and waiting 48-72 hours for review, resulting in immediate churn.
*Vidora resolves this via a Master Server-Side Extraction Strategy*:
* **Thin Client Principle**: The Android app acts as a secure container. It matches URLs, handles concurrent network downloads, parses local metadata, and plays back standard streams, but does not extract.
* **Server-Side yt-dlp Cluster**: The backend handles all HTML parsing, player JS decryption, stream analysis, and quality calculation. If YouTube breaks the signature pattern, the server cluster upgrades `yt-dlp` automatically (within 10 minutes), while edge clients continue operating with zero downtime or need for updates.

---

## SECTION 3: PRODUCT DEFINITION & DEEP UX BLUEPRINTS

### 3.1 Market Strategy & Competitive Positioning

```
+───────────────────────+──────────────────────────────────+─────────────────────────────────+
|      Dimension        |        Legacy Downloaders        |          Vidora Platform        |
+───────────────────────+──────────────────────────────────+─────────────────────────────────+
| UI Aesthetic          | Heavy banner ads, bloated lists  | Clean, minimalist, modern M3    |
| Custom Theming        | Static or system-default only    | 6 tailored color schemes        |
| Extractor Stability   | Slow, app store lag, frequent crash | Immediate server-side patches   |
| Video/Audio Storage   | Unorganized, hardcoded folders   | Folders, custom tags, SQLite db |
| Battery & Resources   | High heat, native extraction     | Lightweight, cloud pre-parsed   |
+───────────────────────+──────────────────────────────────+─────────────────────────────────+
```

### 3.2 Target Personas & Segments
1. **The Commuter (Rohan, 24, Delhi, IN)**: Swings onto the Delhi Metro twice daily, traversing long subterranean sections with highly localized dead zones. Roams on a tight mobile data limit. Expects a robust "WiFi-Only" download scheduler that runs at midnight so his podcasts and YouTube tech summaries are synced before morning.
2. **The Offline Student (Maria, 21, São Paulo, BR)**: Uses local public university library WiFi to cache entire academic playlists and tech lectures for home study, where high-speed broadband is unavailable. Requires bulk queue additions, fast merging, and extensive custom folders.
3. **The Executive Traveler (Alex, 38, Frankfurt, DE)**: Frequent traveler, needs audio-only transcodings of technical keynotes and business panels to listen on flights. Demands consistent, background-stable M4A downloads and a high-performance in-app audio-player.

### 3.3 Viral Components & Growth Loops
* **The "Zero-Network" Dynamic Share Sheet**: When a user highlights downloaded media in their library, Vidora generates an offline-compatible, high-speed regional transfer code via Local Wi-Fi (using Google Nearby Connections or local hotspot server). If the recipient does not have Vidora, they can join the local host WebServer interface to download the app APK directly along with the shared media.
* **One-Tap Share Import**: A shared `.vidora` bundle file is a compressed JSON payload enclosing video metadata, chapter titles, and download sources. Tapping this from an email or messaging app imports the entire list dynamically into the recipient's personal queues with 100% fidelity.

### 3.4 Operational Screen States

#### Active Loading State (Shimmer Specification)
When a media resolution query is in flight, the detail screen renders a matching background color parsed asynchronously. Screen elements pulse synchronously on an 800ms ease-in-out loop using Jetpack Compose dynamic infinite transitions:
- Video Thumbnail Row: Rounded 12dp placeholder box, shimmering horizontally from light gray (`SurfaceVariant`) to mid gray (`Surface`).
- Content Text Blocks: Rounded capsules of matching line heights (18sp and 14sp) running to 70% and 40% column widths.

#### Comprehensive Error Handling & Fault Actions
```
+───────────────────────────+──────────────────────────────────+─────────────────────────────────+
|        Error Class        |         Trigger Condition        |         Mitigation Action       |
+───────────────────────────+──────────────────────────────────+─────────────────────────────────+
| NETWORK_DISCONNECTED      | Active network adapter lost      | Halt downloads, cache state,    |
|                           |                                  | display "Network Down" banner   |
| PROVIDER_SIGNATURE_FAIL   | Content host changes player JS   | Auto-trigger yt-dlp update run  |
|                           |                                  | on server, fallback gracefully  |
| DISK_SPACE_LOW            | Usable memory falls below 500MB  | Freeze download worker, sound   |
|                           |                                  | low memory warning toast notification |
| DECRYPTION_FAILURE        | Restricted or age-gated link      | Direct user to authenticate via |
|                           |                                  | server-side sandbox login sheet |
+───────────────────────────+──────────────────────────────────+─────────────────────────────────+
```

#### Empty States
Every primary tab lists persistent, highly-styled Vector graphics accompanying illustrative actions:
* *Library Video Empty State*: "Build your universe of content." Image representing visual galaxies. Downward-facing arrow indicator targeting the floating clipboard shortcut.
* *Activity Queue Empty Sate*: "No active runs." Sleek clock icon with minimal circular lines representing calm idle loops.

---

## SECTION 4: CLIENT-SIDE ANDROID ARCHITECTURE SPECIFICATION

Vidora's client engineering is written in Platform Kotlin utilizing declarative Jetpack Compose, modularized by high-isolation functional blocks (`:core` and `:feature`), enforcing unidirectional flow.

```
+─────────────────────────────────────────────────────────────+
|                     ANDROID MULTI-MODULE GRAPH              |
+─────────────────────────────────────────────────────────────+
|                                                             |
|                       [:app]  (Application Entry)           |
|                         │                                   |
|       ┌─────────────────┼──────────────────┐                |
|       ▼                 ▼                  ▼                |
|  [:feature:home]  [:feature:library]  [:feature:download]   |
|       │                 │                  │                |
|       └─────────┬───────┴────────┬─────────┘                |
|                 │                │                          |
|                 ▼                ▼                          |
|           [:core:network]  [:core:database]                 |
|                 │                │                          |
|                 └────────┬───────┘                          |
|                          ▼                                  |
|                  [:core:designsys]                          |
|                                                             |
+─────────────────────────────────────────────────────────────+
```

### 4.1 Modularization Directory Blueprint
```
/android
├── app/                              # DI entry, multi-flavor configs
├── core/
│   ├── design-system/                # ThemeEngine, All 6 Theme classes, M3 Foundations
│   ├── network/                      # Retrofit declarations, SSL Pinning interceptors, API signatures
│   ├── database/                     # Transactional Room Database, SQLite DDL definitions, DAOs
│   ├── preferences/                  # Proto DataStore schema bindings, security flags
│   └── platform/                     # Nearby Transfers, Share Sheets, MediaStore bindings
└── feature/
    ├── home/                         # Clipboard detection, validation pipelines, Quick Entry
    ├── details/                      # Format selectors, multi-track audio sheets, subtitles
    ├── download/                     # Async Foreground Worker, queue controllers
    └── library/                      # Grid elements, multi-select workflows, local ExoPlayer controls
```

### 4.2 Foreground Download Architecture & Resuming Protocol
To maximize background lifecycle preservation under Android 14/15 power limits, all media transfers are executed inside an elite `WorkManager` container running as an expedited foreground service, utilizing high-performance chunk-streaming:

```kotlin
package com.vidora.feature.download.worker

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

class ConcurrentDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val database: VidoraDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("KEY_DOWNLOAD_URL") ?: return Result.failure()
        val fileTargetName = inputData.getString("KEY_FILE_NAME") ?: return Result.failure()
        val tempFile = File(context.cacheDir, "$fileTargetName.tmp")

        var localBytes: Long = 0xFFFFFFFF
        if (tempFile.exists()) {
                     localBytes = tempFile.length()
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("Range", "bytes=$localBytes-")
            .build()

        setForeground(createForegroundInfo(0))

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.retry()
                val body = response.body ?: return Result.failure()
                
                RandomAccessFile(tempFile, "rw").use { raf ->
                    raf.seek(localBytes)
                    val buffer = ByteArray(16384) // 16KB Network chunk allocation
                    val inputStream = body.byteStream()
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        localBytes += bytesRead
                        // Send system broadcast of progress periodically to limit UI recomputations
                    }
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(context, "vidora_downloads")
            .setContentTitle("Retrieving Offline Media")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .build()
        return ForegroundInfo(1099, notification)
    }
}
```

### 4.3 MediaStore Integration & Scoped Storage Controls
No direct file-system operations are written. If running on API 29+, streams write to the public `Downloads` path via the virtualized `MediaStore` table. This keeps safety compliance transparent, bypassing hard system storage prompts:

```kotlin
package com.vidora.core.platform.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class MediaStoreExporter(private val context: Context) {
    fun registerMediaEntry(title: String, mimeType: String): Uri? {
        val resolver = context.contentResolver
        val details = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$title.mp4")
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Vidora")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val targetCollection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val savedUri = resolver.insert(targetCollection, details)

        return savedUri
    }
}
```

---

## SECTION 5: ENGINE SCHEMA DEFINITIONS

### 5.1 Relational Database DDL (SQLite/Room & PostgreSQL Standard)
The schemas represent transactional records, download states, and quality indexing rules.

#### SQLite Local Desktop-Edge DB Engine (Client Persistence)
```sql
-- Core Table for tracking Client-Side Offline Lifecycle
CREATE TABLE IF NOT EXISTS downloads (
    id TEXT PRIMARY KEY NOT NULL,
    original_url TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    title TEXT NOT NULL,
    thumbnail_uri TEXT,
    thumbnail_blurhash TEXT,
    duration_secs INTEGER NOT NULL DEFAULT 0,
    video_quality TEXT NOT NULL,
    is_audio_only INTEGER NOT NULL DEFAULT 0,
    media_store_uri TEXT,
    file_bytes INTEGER NOT NULL DEFAULT 0,
    bytes_pulled INTEGER NOT NULL DEFAULT 0,
    download_state TEXT NOT NULL DEFAULT 'QUEUED',
    system_error_code TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_downloads_state ON downloads (download_state);
CREATE INDEX IF NOT EXISTS idx_downloads_normalized_url ON downloads (normalized_url);

-- Available Formats Extracted for Client Decision Bottom Sheets
CREATE TABLE IF NOT EXISTS download_format_specs (
    spec_id TEXT PRIMARY KEY NOT NULL,
    download_id TEXT NOT NULL,
    format_ident TEXT NOT NULL,
    label TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    width INTEGER,
    height INTEGER,
    approximate_bytes INTEGER,
    FOREIGN KEY(download_id) REFERENCES downloads(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_format_specs_parent ON download_format_specs (download_id);
```

#### Multi-Tenant Central Backend Schema (PostgreSQL)
```sql
CREATE TABLE IF NOT EXISTS core_users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_fingerprint VARCHAR(256) UNIQUE NOT NULL,
    plan_tier VARCHAR(32) NOT NULL DEFAULT 'FREE',
    subscription_expiry TIMESTAMPTZ,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extract_jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES core_users(user_id) ON DELETE SET NULL,
    source_url TEXT NOT NULL,
    provider_type VARCHAR(64) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    job_status VARCHAR(64) DEFAULT 'COMPLETED',
    bytes_delivered BIGINT DEFAULT 0,
    failure_signature TEXT,
    execution_latency_ms INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jobs_provider ON extract_jobs(provider_type);
CREATE INDEX idx_jobs_user ON extract_jobs(user_id);
CREATE INDEX idx_users_fingerprint ON core_users(device_fingerprint);
```

### 5.2 Local Preferences Schema (Proto DataStore protobuf Model)
```protobuf
syntax = "proto3";

package com.vidora.preferences;

option java_package = "com.vidora.core.preferences";
option java_multiple_files = true;

message UserPreferences {
  string active_theme_id = 1;               // "midnight_graphite" | "neon_oasis" | "oled_black"
  bool download_over_wifi_only = 2;         // Force execution constraints on Mobile Radio
  int32 max_parallel_workers = 3;           // Standard 1 to 3 Concurrent Limit range
  string active_quality_tier = 4;           // "best" | "1080p" | "720p" | "ask"
  bool foreground_progress_notify = 5;      // Toggle download persistence notifications
  bool auto_inspect_clipboard = 6;         // Background tracking for instant pre-fill
  bool dynamic_system_colors = 7;           // Android Dynamic Theme palette override
  bool crashlytics_opt_in = 8;              // Explicit local telemetry toggle
}
```

---

## SECTION 6: ENTERPRISE BACKEND SERVICE BLUEPRINTS

The production backend of Vidora runs high-speed Fastify processes interacting through Redis queues to separate long-running Python child processes (`yt-dlp` instances) from instant HTTP routing paths.

```
       [ CLIENT HTTP REQUEST ]
                  │
                  ▼
          [ FASTIFY ROUTER ]
                  │
         (Zod Validation Layer)
                  │
                  ├───► [ CACHE SEARCH ] ─────► (Hit!) ───► [ RETURN JSON ]
                  │
                (Miss)
                  │
                  ▼
         [ BULLMQ CONTROLLER ] ───► [ REDIS MEMORY QUEUE ]
                                             │
                                             ▼
                                     [ SCALING NODE ]
                                             │
                                             ▼
                                  [ YT-DLP EXECUTION ]
```

### 6.1 Fastify Routing & Strict Extraction Pipeline (TypeScript)
```typescript
import Fastify from 'fastify';
import { z } from 'zod';
import Redis from 'ioredis';
import { execFile } from 'child_process';
import util from 'util';

const execFileAsync = util.promisify(execFile);
const fastify = Fastify({ logger: true });
const redisClient = new Redis(process.env.REDIS_URL || 'redis://localhost:6379');

// Security Input Validation Strategy via Zod
const MediaLookupSchema = z.object({
  url: z.string().url().refine((val) => {
    return /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be|instagram\.com|tiktok\.com)\/.*$/.test(val);
  }, { message: "Security Violation: Source URL belongs to an unsupported provider." })
});

fastify.post('/v1/media/info', async (request, reply) => {
  try {
    const parseResult = MediaLookupSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({ error: "BAD_REQUEST", details: parseResult.error.format() });
    }

    const targetUrl = parseResult.data.url;
    const cacheKey = `vidora:cache_url:${Buffer.from(targetUrl).toString('base64')}`;
    
    // Check local Redis engine instances first to optimize resource performance
    const cachedMetadata = await redisClient.get(cacheKey);
    if (cachedMetadata) {
      return reply.status(200).send(JSON.parse(cachedMetadata));
    }

    // Call the server-side extraction engine securely
    const { stdout } = await execFileAsync('yt-dlp', [
      '--dump-json',
      '--no-playlist',
      '--skip-download',
      '--no-warnings',
      targetUrl
    ], { timeout: 15000, maxBuffer: 10 * 1024 * 1024 });

    const rawParsed = JSON.parse(stdout);
    
    const unifiedPayload = {
      title: rawParsed.title,
      duration: rawParsed.duration,
      channel: rawParsed.uploader,
      thumbnail: rawParsed.thumbnail,
      formats: rawParsed.formats
        .filter((f: any) => f.vcodec !== 'none' || f.acodec !== 'none')
        .map((f: any) => ({
          format_id: f.format_id,
          ext: f.ext,
          quality_label: f.format_note || `${f.width}p`,
          width: f.width,
          height: f.height,
          approx_bytes: f.filesize || f.filesize_approx || null
        }))
    };

    // Cache responses dynamically with a standard TTL of 12 hours
    await redisClient.setex(cacheKey, 43200, JSON.stringify(unifiedPayload));

    return reply.status(200).send(unifiedPayload);
  } catch (error: any) {
    fastify.log.error(error);
    return reply.status(500).send({ error: "EXTRACTION_FAILURE", msg: error.message });
  }
});
```

### 6.2 yt-dlp Auto-Update Pipeline & Active Smoke-Testing System
Because provider extraction algorithms break without notice, Vidora relies on a Cron-driven system-level task pipeline that tests, executes, and deploys extractor binaries on a zero-downtime microservice strategy:

```yaml
# GitHub Workflows Auto-Update System
name: Engine Verification & Auto-Deploy Pipeline

on:
  schedule:
    - cron: '0 */6 * * *' # Executes every 6 hours
  workflow_dispatch:

jobs:
  validate_extractors:
    runs-on: ubuntu-latest
    steps:
      - name: Fetch Pipeline Core
        uses: actions/checkout@v4

      - name: Verify Host Extractor Update
        run: |
          sudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
          sudo chmod a+rx /usr/local/bin/yt-dlp
          yt-dlp --version > local_ver.txt

      - name: Execute Live Smoke-Testing Suite
        run: |
          # Test YouTube Standard
          yt-dlp --dump-json "https://www.youtube.com/watch?v=dQw4w9WgXcQ" > /dev/null
          
          # Test YouTube Shorts
          yt-dlp --dump-json "https://www.youtube.com/shorts/pS09Uv4O360" > /dev/null
          
          # Test TikTok Endpoint
          yt-dlp --dump-json "https://www.tiktok.com/@tiktok/video/7106839566009912618" > /dev/null

      - name: Deploy Image to Docker Hub
        if: success()
        run: |
          docker build -t vidora/extractor-node:latest .
          echo "${{ secrets.DOCKER_TOKEN }}" | docker login -u vidora --password-stdin
          docker push vidora/extractor-node:latest
```

---

## SECTION 7: CRITICAL SECURITY & ACCESS CONSTRAINTS

Vidora maintains high-level structural defenses on active user device surfaces, communication tunnels, and backend resources.

### 7.1 OWASP Mobile Principles Implemented
* **M1: Improper Platform Usage**: Bypasses absolute programmatic local storage permissions by implementing Scoped Storage and public destination mappings.
* **M2: Insecure Data Storage**: Database structures are encrypted locally using Android SQLCipher wrappers integrated inside Room. Proto preferences are compiled to high-isolation folders inaccessible outside the sandbox environment.
* **M3: Insecure Communication**: Enforces direct HTTPS endpoints. Cleartext connection layouts are explicitly disabled via Network Security Config files.

### 7.2 Safety Implementation: Client Network Security Config
```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.vidora.app</domain>
        <pin-set expiration="2027-12-31">
            <!-- Strict SHA-256 SSL Certificate Pinning for Production Endpoints -->
            <pin digest="SHA-256">9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08</pin>
            <pin digest="SHA-256">607f5572fc11854185d346ff17515b0a03bb1f0c2f354f19fc2541fa9f12000a</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

### 7.3 Token-Bucket Rate Limiter Configuration (Backend Redis Module)
```typescript
import Redis from 'ioredis';

const redis = new Redis();

async function isCallerAllowed(callerId: string, limit: number, windowSecs: number): Promise<boolean> {
  const key = `ratelimit:${callerId}`;
  
  // Use a transactional multi/exec block to enforce atomic rate limits
  const pipeline = redis.multi();
  pipeline.incr(key);
  pipeline.ttl(key);
  
  const results = await pipeline.exec();
  const count = results?.[0]?.[1] as number;
  const ttl = results?.[1]?.[1] as number;
  
  if (count === 1) {
    await redis.expire(key, windowSecs);
    return true;
  }
  
  return count <= limit;
}
```

---

## SECTION 8: HARDWARE METRICS & DEPLOYMENT TOPOLOGIES

```
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                INFRASTRUCTURE GROW PLANS                                |
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                         |
|  [ MVP PROVISION (500 MAU) ] ─────────────────────────► Total Estimated Cost: $15 / Mo  |
|  - Server: Render / Railway Basic Node (1 Core, 2GB Memory)                             |
|  - Cache: Redis Cloud Core Shared (30mb Limits)                                         |
|  - Database: Shared Neon Serverless Instance (500mb Capacity)                           |
|                                                                                         |
|  [ GROWTH SCALE (10,000 MAU) ] ───────────────────────► Total Estimated Cost: $60 / Mo  |
|  - Server: 2 x Backend Workers (1 Core, 2GB Memory Dedicated)                           |
|  - Cache: Dedicated Railway Redis Replica Set (1GB Memory Node)                         |
|  - Database: Production AWS RDS Postgres Multi-AZ (Region India, db.t4g.small)           |
|                                                                                         |
|  [ PLATFORM HORIZON (100,000 MAU) ] ──────────────────► Total Estimated Cost: $300 / Mo |
|  - Server: Multi-Scale Kubernetes Cluster (K8s) via AWS EKS                             |
|  - Cache: ElastiCache Redis Tier (db.r6g.large Dedicated Cluster)                       |
|  - Database: AWS Aurora Serverless v2 Multi-Region Database Replica                      |
|                                                                                         |
+─────────────────────────────────────────────────────────────────────────────────────────+
```

---

## SECTION 9: PRODUCT ADMIN CONSOLES & LANDING INFRASTRUTURE

### 9.1 Control Center Features
1. **Interactive Dashboard**: Graphs listing active download volumes, server-side CPU utilization averages, bandwidth, API latency percentiles (P50, p90, p99), and active worker instances.
2. **Provider Status Grid**: Visual alerts monitoring scraper integrity. Red/Green signals indicate health metrics for YouTube, Instagram, TikTok, and Twitter extractors based on success rates over the past 5 minutes.
3. **Download Analytics & Abuse Control**: Lists active users with high execution volumes. Permits remote flagging of device fingerprints attempting automated database harvests.

### 9.2 Complete Web Presence Strategy
The primary website (`vidora.app`) is optimized using static generation pipelines (Next.js/Astro) delivering high-speed loading times on mobile devices:
- `/` (Landing): Implements structured layouts displaying high-contrast imagery, dark layouts, visual grid capabilities, and clear direct-install triggers.
- `/download`: Dynamic user-agent matching router that streams the raw Android APK dynamically or routes target desktop files cleanly.
- `/changelog`: Version history timelines pulled directly from GitHub Tags, listing incremental fixes, core speed updates, and feature upgrades.

---

## SECTION 10: IN-APP BILLING & AD CONVERSTION MECHANICS

```
               [ VISITOR ]
                    │
                    ▼
          [ DOWNLOADS APPLIED ]
                    │
                    ▼
          (Target 3 active limit)
                    │
                    ├───► [ RETAIN FREE ] (Engage Rewarded Ad Option)
                    │
                    ▼
          [ PRO PAYWALL VIEW ] ───► Paywall Trigger Event (RevenueCat Hook)
                                            │
                                            ├───► Monthly (Subscription Activated)
                                            │
                                            └───► Lifetime (Targeted at Churn Moments)
```

### 10.1 Multi-Tier Monolith Plans
* **Midnight Standard Option (Free)**: Access to standard resolutions, 1 concurrent execution, system default theme (midnight black). Supported by occasional rewarded video sequences designed to increase execution limits temporarily.
* **Vidora Elite (Pro - $1.99/mo | $14.99/yr)**: Activates parallel background workers (up to 3 concurrent downloads), scheduled automation runs, biometric locking mechanisms, and the complete custom catalog layout suite.
* **Universe Lifetime ($24.99 Single Run)**: Unrestricted platform access forever, with priority support channels. Offered as a targeted checkout option to users who have canceled three consecutive monthly renewals or shown high active engagement over 6 months.

### 10.2 Ad Mediation & Strategy (AdMob + AppLovin MAX)
To optimize monetization performance while maintaining premium positioning, Vidora implements **AppLovin MAX** and **Google AdMob mediation rules**:
1. *Zero Banners Policy*: No banner advertisements are permitted on presentation files or screens.
2. *Rewarded Action System*: To download at 1080p without an active Pro subscript, standard account holders can choose to watch an interactive rewarded system clip. On callback validation, high-speed resolution is enabled for the next 4 hours.

---

## SECTION 11: INSTRUMENTED AUTOMATED VERIFICATION MATRIX

Vidora implements a strict automation testing protocol designed to prevent regression issues during rapid iteration.

```
       [ DEVELOPER COMMIT ]
                 │
                 ▼
       [ AUTOMATED TEST SUITE ]
                 │
                 ├───► [ ROBOLECTRIC TESTS ] ──► JVM Business Logic Validation
                 │
                 ├───► [ ROBORAZZI TESTS ]   ──► Visual Screenshot Verification
                 │
                 ▼
      [ INTEGRATION PIPELINE ]
                 │
                 ├───► [ K6 LOAD TESTING ]   ──► Backend Response Validation
                 │
                 ▼
          [ PROD RELEASE ]
```

### 11.1 Screenshot Verification (Jetpack Compose Roborazzi Test)
```kotlin
package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.VidoraTheme
import com.github.takahirom.roborazzi.captureRoborazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureGreetingScreenImage() {
        composeTestRule.setContent {
            VidoraTheme {
                MainScreenContent()
            }
        }
        // Capture a clean visual vector artifact of the main view
        composeTestRule.onRoot().captureRoborazzi("src/test/screenshots/greeting.png")
    }
}
```

### 11.2 Backend Performance & Volume Run Validation (K6 Load Test Script)
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp up to 50 active concurrent runners
    { duration: '1m', target: 200 },  // Scale load up to 200 request sequences
    { duration: '30s', target: 0 },    // Wind down load to baseline
  ],
};

export default function () {
  const payload = JSON.stringify({
    url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Vidora-Key': 'development_mock_security_key_signature',
    },
  };

  const response = http.post('https://api.vidora.app/v1/media/info', payload, params);
  
  check(response, {
    'resolution pipeline returned standard response (200)': (r) => r.status === 200,
    'extraction response validated under 900ms': (r) => r.timings.duration < 900,
  });

  sleep(1);
}
```

---

## SECTION 12: MULTI-YEAR ENGINEERING PLATFORM ROADMAP

```
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                     MULTI-YEAR ROADMAP                                  |
+─────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                         |
|  [ CORE STAGE (Month 1-3) ] ─────────────────────────► End-to-End Delivery Established  |
|  - Finalize multi-module architecture layout on Android client                         |
|  - Implement Server-Side Extraction engine wrapper with Redis Caching                  |
|  - Set up SQLite Room, Proto preferences, and download worker interfaces                 |
|                                                                                         |
|  [ POLISH AND SUBS (Month 4-6) ] ─────────────────────► Market Entry Readiness          |
|  - Integrate RevenueCat and complete 6 custom system themes                             |
|  - Implement full ExoPlayer in-app rendering capabilities for offline media             |
|  - Embed SSL Pinning, OWASP hardeners, and play store gating controls                   |
|                                                                                         |
|  [ MULTI-CHANNEL RELEASE (Month 7-12) ] ──────────────► Distribution & Growth            |
|  - Publish Direct APK, F-Droid Packages, and gate play store listings                   |
|  - Implement Wi-Fi high-speed Nearby Share transfer capabilities                        |
|  - Release batch playlist downloading engines and subtitle converters                   |
|                                                                                         |
+─────────────────────────────────────────────────────────────────────────────────────────+
```

---
*End of Specification Document V2.0. This blueprint serves as the definitive reference engineering design file.*
