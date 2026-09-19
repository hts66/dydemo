"""统计所有 result_*.jtl 的并发压测指标，输出汇总表格。

用法: python summary.py
"""
import csv
import glob
import os

def pct(sorted_vals, p):
    if not sorted_vals:
        return 0
    idx = min(len(sorted_vals) - 1, int(len(sorted_vals) * p))
    return sorted_vals[idx]

def summarize(path):
    elapsed = []
    latency = []
    errors = 0
    total = 0
    bytes_total = 0
    t_first = None
    t_last = None
    with open(path, encoding="utf-8", errors="replace") as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                e = int(row["elapsed"])
                l = int(row["Latency"])
                b = int(row["bytes"])
                ts = int(row["timeStamp"])
                ok = row["success"] == "true"
            except (ValueError, KeyError):
                continue
            total += 1
            if not ok:
                errors += 1
            elapsed.append(e)
            latency.append(l)
            bytes_total += b
            if t_first is None or ts < t_first:
                t_first = ts
            if t_last is None or ts > t_last:
                t_last = ts
    if total == 0:
        return None
    elapsed.sort()
    latency.sort()
    dur = max((t_last - t_first) / 1000.0, 1.0)
    return {
        "samples": total,
        "errors": errors,
        "err%": errors * 100.0 / total,
        "dur_s": dur,
        "rps": total / dur,
        "avg_ms": sum(elapsed) / len(elapsed),
        "p90_ms": pct(elapsed, 0.90),
        "p95_ms": pct(elapsed, 0.95),
        "p99_ms": pct(elapsed, 0.99),
        "max_ms": elapsed[-1],
        "ttfb_avg": sum(latency) / len(latency),
        "ttfb_p99": pct(latency, 0.99),
        "mb": bytes_total / 1024 / 1024,
        "mbps": bytes_total / 1024 / 1024 / dur,
    }

files = sorted(glob.glob("result_*.jtl"))
for path in files:
    s = summarize(path)
    if not s:
        print(f"{path}: no valid samples")
        continue
    print(f"{os.path.basename(path)}:")
    print(f"  samples={s['samples']} errors={s['errors']} ({s['err%']:.2f}%) "
          f"duration={s['dur_s']:.0f}s rps={s['rps']:.1f}")
    print(f"  elapsed: avg={s['avg_ms']:.0f}ms p90={s['p90_ms']:.0f} p95={s['p95_ms']:.0f} "
          f"p99={s['p99_ms']:.0f} max={s['max_ms']:.0f}")
    print(f"  TTFB:    avg={s['ttfb_avg']:.0f}ms p99={s['ttfb_p99']:.0f}  "
          f"traffic={s['mb']:.0f}MB  {s['mbps']:.1f}MB/s")
    print()
