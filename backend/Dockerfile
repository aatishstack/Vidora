FROM node:22-slim

# System dependencies for executing yt-dlp python core and processing merges
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install latest yt-dlp release binary
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

# Compile TS code if needed, but we output standard node launcher for prod:
RUN npm install -g typescript && tsc

EXPOSE 8080

CMD ["node", "dist/app.js"]
