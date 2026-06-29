"use client";

import { Clock } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Badge } from "@/components/ui/badge";

// Ticks every second toward `deadline` (epoch ms) and fires `onExpire` once at zero — the wizard
// uses it to auto-submit a timed attempt. The callback is read through a ref so a changing handler
// identity never restarts the interval (deadline is the only real dependency).
export function AttemptTimer({ deadline, onExpire }: { deadline: number; onExpire: () => void }) {
  const [remaining, setRemaining] = useState(() => Math.max(0, deadline - Date.now()));
  const onExpireRef = useRef(onExpire);
  const firedRef = useRef(false);

  useEffect(() => {
    onExpireRef.current = onExpire;
  });

  useEffect(() => {
    function tick() {
      const next = Math.max(0, deadline - Date.now());
      setRemaining(next);
      if (next <= 0 && !firedRef.current) {
        firedRef.current = true;
        onExpireRef.current();
      }
    }
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [deadline]);

  const totalSeconds = Math.ceil(remaining / 1000);
  const mm = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const ss = String(totalSeconds % 60).padStart(2, "0");
  const low = remaining <= 60_000;

  return (
    <Badge variant={low ? "destructive" : "secondary"} className="gap-1 font-mono tabular-nums">
      <Clock className="size-3" />
      {mm}:{ss}
    </Badge>
  );
}
