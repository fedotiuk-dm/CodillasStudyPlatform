import { useEffect, useRef } from "react";

/**
 * A short two-tone notification chime via the Web Audio API — no audio asset needed. Returns a
 * `playNotification()` to fire when a new notification arrives (e.g. the unread count goes up).
 */
export function useNotificationSound() {
  const playRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (typeof globalThis.AudioContext === "undefined") return;
    const ctx = new AudioContext();

    playRef.current = () => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.setValueAtTime(830, ctx.currentTime);
      osc.frequency.setValueAtTime(990, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.3);
    };

    return () => {
      void ctx.close();
    };
  }, []);

  return { playNotification: () => playRef.current?.() };
}
