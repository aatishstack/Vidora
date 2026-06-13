"use client";

import React, { useState } from 'react';
import { 
  BarChart3, 
  Activity, 
  Settings, 
  Database, 
  ShieldCheck, 
  RefreshCw, 
  AlertTriangle,
  Flame,
  CloudLightning,
  TrendingUp,
  Sliders,
  Users
} from 'lucide-react';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'status' | 'analytics' | 'providers'>('status');
  const [providerFlags, setProviderFlags] = useState({
    youtube: true,
    instagram: true,
    tiktok: false
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans p-6">
      {/* Upper Navigation Row */}
      <header className="flex justify-between items-center pb-8 border-b border-slate-800">
        <div>
          <h1 className="text-2xl font-black text-violet-400 tracking-wider flex items-center gap-2">
            <Flame className="text-violet-500 fill-violet-500" /> VIDORA ADMIN PANEL
          </h1>
          <p className="text-sm text-slate-400">Production Control and Extractor Orchestration Suite</p>
        </div>
        <div className="flex gap-4 items-center">
          <span className="flex items-center gap-2 text-xs bg-emerald-950 border border-emerald-800 text-emerald-400 px-3 py-1 rounded-full font-bold">
            <Activity className="animate-pulse size-3" /> Core: Operational
          </span>
          <span className="text-xs text-slate-500">v2.0.0</span>
        </div>
      </header>

      <main className="pt-8 grid grid-cols-1 md:grid-cols-4 gap-6">
        {/* Metric Cards */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex justify-between items-start text-xs font-black text-slate-400 tracking-wider uppercase">
            <span>Server Bandwidth (24h)</span>
            <TrendingUp className="text-emerald-500" />
          </div>
          <p className="text-3xl font-black pt-4">42.8 GB/s</p>
          <div className="text-[11px] text-emerald-400 pt-1">+12.4% vs last week</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex justify-between items-start text-xs font-black text-slate-400 tracking-wider uppercase">
            <span>Avg API Latency</span>
            <Activity className="text-violet-500" />
          </div>
          <p className="text-3xl font-black pt-4">320 ms</p>
          <div className="text-[11px] text-violet-400 pt-1">P95: 540ms · Cloudcached</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex justify-between items-start text-xs font-black text-slate-400 tracking-wider uppercase">
            <span>Resolutions Today</span>
            <Flame className="text-amber-500" />
          </div>
          <p className="text-3xl font-black pt-4">124,580</p>
          <div className="text-[11px] text-slate-400 pt-1">99.8% Successful Runs</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex justify-between items-start text-xs font-black text-slate-400 tracking-wider uppercase">
            <span>Active Users</span>
            <Users className="text-blue-500" />
          </div>
          <p className="text-3xl font-black pt-4">84,203</p>
          <div className="text-[11px] text-blue-400 pt-1">+4.2% concurrent load</div>
        </div>

        {/* Primary Layout sections */}
        <section className="md:col-span-3 bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex gap-4 border-b border-slate-800 pb-4 mb-6">
            <button 
              onClick={() => setActiveTab('status')}
              className={`pb-2 px-1 text-sm font-bold border-b-2 transition-all ${activeTab === 'status' ? 'border-violet-500 text-violet-400' : 'border-transparent text-slate-400'}`}
            >
              System Events log
            </button>
            <button 
              onClick={() => setActiveTab('providers')}
              className={`pb-2 px-1 text-sm font-bold border-b-2 transition-all ${activeTab === 'providers' ? 'border-violet-500 text-violet-400' : 'border-transparent text-slate-400'}`}
            >
              Extraction Providers
            </button>
          </div>

          {activeTab === 'status' ? (
            <div className="space-y-4">
              <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800 text-sm">
                <span className="text-xs bg-emerald-950 border border-emerald-900 text-emerald-400 px-2 py-0.5 rounded font-black font-mono">SYS</span>
                <div>
                  <p className="font-bold">yt-dlp sub-process execution updated automatically</p>
                  <p className="text-xs text-slate-400 pt-1">New binary release 2026.06.13 extracted & smoke tests verified successfully.</p>
                </div>
              </div>
              <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800 text-sm">
                <span className="text-xs bg-violet-950 border border-violet-900 text-violet-400 px-2 py-0.5 rounded font-black font-mono">CACHE</span>
                <div>
                  <p className="font-bold">Redis instance clean completed</p>
                  <p className="text-xs text-slate-400 pt-1">Flushed 1,240 expired metadata streams tags dynamically.</p>
                </div>
              </div>
              <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800 text-sm">
                <span className="text-xs bg-amber-950 border border-amber-900 text-amber-500 px-2 py-0.5 rounded font-black font-mono">GATEWAY</span>
                <div>
                  <p className="font-bold flex items-center gap-2 text-amber-500"><AlertTriangle className="size-4" /> TikTok provider degraded triggers</p>
                  <p className="text-xs text-slate-400 pt-1">Detection engine is logging 18% parsing failures. Fail-over circuit breaker active.</p>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">Enable/Disable Provider Extractors</p>
              <div className="space-y-4">
                {Object.entries(providerFlags).map(([key, value]) => (
                  <div key={key} className="flex items-center justify-between p-4 bg-slate-950 border border-slate-800 rounded-xl">
                    <div>
                      <p className="font-bold capitalize">{key} Endpoint</p>
                      <p className="text-xs text-slate-400 font-mono">/v1/media/info/resolve::{key}</p>
                    </div>
                    <button 
                      onClick={() => setProviderFlags(prev => ({...prev, [key]: !value}))}
                      className={`px-4 py-2 rounded-xl text-xs font-black tracking-wider transition-all uppercase ${value ? 'bg-emerald-600 text-white hover:bg-emerald-500' : 'bg-rose-950 text-rose-500 border border-rose-900'}`}
                    >
                      {value ? 'Active (Green)' : 'Disabled'}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>

        {/* Sidebar settings */}
        <aside className="bg-slate-900 border border-slate-800 rounded-2xl p-6 h-fit space-y-6">
          <h2 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
            <Sliders className="size-4 text-violet-500" /> Platform Controls
          </h2>
          <div className="space-y-4 text-sm">
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 select-none">
              <p className="font-bold flex items-center gap-2"><Database className="size-4 text-violet-400" /> PostgreSQL Node</p>
              <p className="text-xs text-slate-400 pt-1">Instance: Primary Replica AZ</p>
            </div>
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 select-none">
              <p className="font-bold flex items-center gap-2"><CloudLightning className="size-4 text-amber-500" /> Rate Limits</p>
              <p className="text-xs text-slate-400 pt-1">Limits: 20 per/IP/Minute maximum</p>
            </div>
          </div>
        </aside>
      </main>
    </div>
  );
}
