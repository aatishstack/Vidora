"use client";

import React from 'react';
import { 
  Flame, 
  Smartphone, 
  Sparkles, 
  ShieldAlert, 
  Check, 
  ArrowRight, 
  Download, 
  HelpCircle,
  FolderLock
} from 'lucide-react';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 font-sans selection:bg-violet-500 selection:text-white">
      {/* Header Grid */}
      <header className="max-w-7xl mx-auto px-6 py-6 flex justify-between items-center border-b border-neutral-800">
        <div className="flex items-center gap-2 font-black text-xl tracking-wider text-violet-400">
          <Flame className="text-violet-500 fill-violet-500" /> VIDORA
        </div>
        <div className="flex gap-4">
          <a href="#features" className="text-sm text-neutral-400 hover:text-white transition-all pt-2">Features</a>
          <a href="#faq" className="text-sm text-neutral-400 hover:text-white transition-all pt-2">FAQ</a>
          <a href="#download" className="bg-violet-600 px-4 py-2 rounded-xl text-sm font-black hover:bg-violet-500 transition-all text-white">Direct APK Download</a>
        </div>
      </header>

      {/* Hero Sector */}
      <section className="max-w-4xl mx-auto text-center px-6 py-24 space-y-8">
        <div className="inline-flex items-center gap-2 bg-neutral-900 border border-neutral-800 px-4 py-1.5 rounded-full text-xs text-neutral-400">
          <Sparkles className="size-4 text-violet-400" /> Introducing Vidora Platform Gold Release
        </div>
        <h1 className="text-5xl md:text-7xl font-black tracking-tight leading-none">
          Your personal <span className="text-transparent bg-clip-text bg-gradient-to-r from-violet-400 to-indigo-500">offline library</span>, perfectly mapped.
        </h1>
        <p className="text-lg md:text-xl text-neutral-400 max-w-2xl mx-auto font-medium">
          Retrieve, aggregate, and browse offline video content from any major platform without tracking ads, performance lags, or layout bloat. Created for high-performance edge storage.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4" id="download">
          <a 
            href="/downloads/vidora-v2.0.apk" 
            className="bg-violet-600 hover:bg-violet-500 text-white font-bold px-8 py-4 rounded-2xl shadow-lg hover:shadow-violet-900/40 transition-all flex items-center justify-center gap-3"
          >
            <Download className="size-5" /> Download Android APK (Gold v2.0)
          </a>
          <a 
            href="https://github.com/vidora/app/releases" 
            className="bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 text-neutral-200 font-bold px-8 py-4 rounded-2xl transition-all flex items-center justify-center gap-2"
          >
            Browse GitHub Releases <ArrowRight className="size-4" />
          </a>
        </div>
      </section>

      {/* Grid Highlights section */}
      <section className="bg-neutral-900 border-y border-neutral-800 py-16" id="features">
        <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="bg-neutral-950 p-8 rounded-2xl border border-neutral-800 space-y-4">
            <div className="size-12 rounded-xl bg-violet-950 border border-violet-900 flex items-center justify-center text-violet-400">
              <Smartphone />
            </div>
            <h3 className="text-lg font-bold">Fluid 90FPS Native Compose</h3>
            <p className="text-sm text-neutral-400 leading-relaxed">
              Engineered exclusively in platform Kotlin. Full screen predictive gestures, deep hardware accelerated VideoViews, and dynamic, seamless Material 3 interfaces.
            </p>
          </div>

          <div className="bg-neutral-950 p-8 rounded-2xl border border-neutral-800 space-y-4">
            <div className="size-12 rounded-xl bg-emerald-950 border border-emerald-900 flex items-center justify-center text-emerald-400">
              <ShieldAlert />
            </div>
            <h3 className="text-lg font-bold">Zero-Downtime Server Extraction</h3>
            <p className="text-sm text-neutral-400 leading-relaxed">
              Scraping algorithms are isolated inside docker clusters. In case of platform breaking, updates apply on-air globally under 10 minutes with 0 edge client upgrades required.
            </p>
          </div>

          <div className="bg-neutral-950 p-8 rounded-2xl border border-neutral-800 space-y-4">
            <div className="size-12 rounded-xl bg-indigo-950 border border-indigo-900 flex items-center justify-center text-indigo-400">
              <FolderLock />
            </div>
            <h3 className="text-lg font-bold">Personal Vault Organization</h3>
            <p className="text-sm text-neutral-400 leading-relaxed">
              No remote histories stored. All user data is encrypted and persistent inside secure Room SQLite buffers. Safeguards your file library metrics with privacy seals.
            </p>
          </div>
        </div>
      </section>

      {/* FAQ / Q&A Accordion */}
      <section className="max-w-4xl mx-auto px-6 py-24 space-y-12" id="faq">
        <h2 className="text-3xl font-black text-center tracking-tight flex items-center justify-center gap-2"><HelpCircle className="text-violet-500" /> Platform FAQ</h2>
        <div className="space-y-6">
          <div className="p-6 bg-neutral-900/50 rounded-2xl border border-neutral-800">
            <h4 className="font-bold text-base">Why download directly from APK?</h4>
            <p className="text-sm text-neutral-400 pt-2 leading-relaxed">
              To guarantee complete, unrestricted, high-speed formats selections on India networks and globally. Sideloading the signed direct-target platform APK avoids restrictive sandbox gates while preserving security.
            </p>
          </div>
          <div className="p-6 bg-neutral-900/50 rounded-2xl border border-neutral-800">
            <h4 className="font-bold text-base">Is Vidora fully compliant?</h4>
            <p className="text-sm text-neutral-400 pt-2 leading-relaxed">
              Yes. The package acts as a high-fidelity local content file cataloger. It matches URLs cleanly, and respects standard device constraints. The user preserves liability of cached materials content.
            </p>
          </div>
        </div>
      </section>

      {/* Static Footer */}
      <footer className="border-t border-neutral-800 py-12 text-center text-neutral-500 text-xs space-y-4">
        <p className="font-semibold tracking-widest uppercase">Vidora Platform © 2026. All Rights Reserved.</p>
        <p className="max-w-xl mx-auto leading-relaxed">
          Vidora is an open-source structural utility. Any content retrieved via yt-dlp is for personal backup storage. We are not allied or associated with any targeted third-party content platforms names.
        </p>
      </footer>
    </div>
  );
}
