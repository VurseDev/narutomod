"""Reproduce the original lightning SFX. No recordings or third-party samples.

Requires numpy and soundfile (libsndfile with Ogg/Vorbis support).
Writes only generated audio below assets/narutomod/sounds/custom_jutsu.
Run from any directory; --verify measures the decoded Ogg files without changing them.
"""

import argparse
import json
import math
from pathlib import Path

import numpy as np
import soundfile as sf


RATE = 44100
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/narutomod/sounds/custom_jutsu"


def lowpass(signal, frequency):
    """Deterministic one-pole filter; no scipy dependency."""
    alpha = 1.0 - math.exp(-2.0 * math.pi * frequency / RATE)
    result = np.empty_like(signal)
    previous = 0.0
    for i, value in enumerate(signal):
        previous += alpha * (value - previous)
        result[i] = previous
    return result


def band(signal, low, high):
    return lowpass(signal - lowpass(signal, low), high)


def tone(t, start, end):
    progress = t / max(t[-1], 1.0 / RATE)
    frequency = start + (end - start) * progress
    return np.sin(2.0 * math.pi * np.cumsum(frequency) / RATE)


def sparks(t, rng, count, gain=1.0, rising=False):
    result = np.zeros(len(t))
    for _ in range(count):
        when = rng.uniform(0.02, t[-1] - 0.01)
        if rising:
            when = math.sqrt(when / t[-1]) * t[-1]
        start = int(when * RATE)
        n = min(int(rng.uniform(0.006, 0.025) * RATE), len(t) - start)
        local = np.arange(n) / RATE
        burst = rng.normal(size=n) * np.exp(-local * rng.uniform(190, 430))
        burst += 0.25 * np.sin(2 * math.pi * rng.uniform(1500, 4900) * local) * np.exp(-local * 220)
        result[start:start + n] += burst * gain * rng.uniform(0.25, 0.75)
    return band(result, 650, 10500)


def finish(signal):
    signal = signal - lowpass(signal, 28)
    signal = np.tanh(signal * 1.3)
    # Short endpoint fades avoid clicks not belonging to the intentional electric transients.
    edge = min(180, len(signal) // 8)
    signal[:edge] *= np.linspace(0, 1, edge)
    signal[-edge:] *= np.linspace(1, 0, edge)
    # Leave >3 dB of peak headroom for Vorbis reconstruction and battle mixing.
    signal *= 0.64 / max(np.max(np.abs(signal)), 1e-9)
    return signal.astype(np.float32)


def synthesize(kind, duration, seed):
    rng = np.random.default_rng(seed)
    t = np.arange(int(duration * RATE), dtype=np.float64) / RATE
    p = t / duration
    noise = rng.normal(size=len(t))
    fizz = band(noise, 1800, 10000)
    body = band(noise, 110, 1450)
    if kind == "charge":
        envelope = (np.sin(np.minimum(p * 1.03, 1) * math.pi) ** 0.55) * (0.20 + p * 0.80)
        signal = (0.19 * tone(t, 160, 690) + 0.075 * tone(t, 460, 1780)) * envelope
        pulse = 0.5 + 0.5 * np.sin(2 * math.pi * (9 * t + 28 * t * t)) ** 6
        signal += 0.24 * fizz * envelope * pulse + sparks(t, rng, 23, 0.44, True)
    elif kind == "discharge":
        signal = 0.83 * fizz * np.exp(-t * 38) + 0.70 * body * np.exp(-t * 11)
        signal += 0.26 * tone(t, 290, 70) * np.exp(-t * 13)
        signal += 0.12 * tone(t, 1570, 470) * np.exp(-t * 8)
        signal += sparks(t, rng, 12, 0.6) * np.exp(-t * 4)
    elif kind == "pillar_rise":
        envelope = np.sin(p * math.pi) ** 1.2
        signal = 0.48 * body * envelope * (0.65 + 0.35 * np.sin(t * 57) ** 2)
        signal += 0.17 * tone(t, 68, 125) * envelope
        signal += 0.18 * fizz * envelope * p + sparks(t, rng, 30, 0.45, True)
        signal += 0.065 * tone(t, 340, 870) * envelope
    elif kind == "pillar_impact":
        signal = 1.0 * fizz * np.exp(-t * 50)
        signal += 0.86 * body * np.exp(-t * 9.0)
        signal += 0.42 * tone(t, 103, 47) * np.exp(-t * 9)
        signal += 0.23 * band(noise, 45, 430) * np.exp(-t * 4.2)
        signal += 0.13 * tone(t, 820, 190) * np.exp(-t * 7)
        signal += sparks(t, rng, 20, 0.64) * np.exp(-t * 3)
        # Two quiet slap tails suggest the discharge spreading through the cage.
        for delay, amplitude in ((0.075, 0.18), (0.15, 0.10)):
            offset = int(delay * RATE)
            signal[offset:] += amplitude * signal[:-offset].copy()
    elif kind == "chain_snap":
        signal = 0.90 * fizz * np.exp(-t * 62) + 0.38 * body * np.exp(-t * 33)
        signal += 0.18 * tone(t, 1900, 340) * np.exp(-t * 18)
        signal += sparks(t, rng, 6, 0.28) * np.exp(-t * 12)
    elif kind == "sustain":
        envelope = np.sin(p * math.pi) ** 0.7
        signal = 0.085 * tone(t, 108, 112) * envelope
        signal += 0.17 * fizz * envelope * (0.25 + 0.75 * np.sin(t * 91) ** 8)
        signal += sparks(t, rng, 18, 0.30) * envelope
    else:
        raise ValueError(kind)
    return finish(signal)


SPECS = (
    ("charge", 0.72, 731),
    ("discharge_a", 0.54, 182),
    ("discharge_b", 0.57, 195),
    ("pillar_rise", 0.90, 214),
    ("pillar_impact_a", 1.05, 310),
    ("pillar_impact_b", 0.98, 319),
    ("chain_snap_a", 0.23, 451),
    ("chain_snap_b", 0.26, 458),
    ("sustain", 0.56, 682),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    if not args.verify:
        OUT.mkdir(parents=True, exist_ok=True)
        for name, duration, seed in SPECS:
            kind = name.rsplit("_", 1)[0] if name.endswith(("_a", "_b")) else name
            sf.write(OUT / (name + ".ogg"), synthesize(kind, duration, seed), RATE,
                     format="OGG", subtype="VORBIS")
    report = []
    for name, _, _ in SPECS:
        path = OUT / (name + ".ogg")
        data, rate = sf.read(path)
        peak = float(np.max(np.abs(data)))
        rms = float(np.sqrt(np.mean(data * data)))
        assert data.ndim == 1 and rate == RATE, "Positional SFX must remain 44.1 kHz mono"
        assert np.isfinite(data).all() and peak < 0.90, "Nonfinite samples or insufficient headroom"
        assert abs(float(np.mean(data))) < 0.002, "Excessive DC offset"
        report.append({"file": name + ".ogg", "seconds": round(len(data) / rate, 3),
                       "peak_dbfs": round(20 * math.log10(max(peak, 1e-12)), 2),
                       "rms_dbfs": round(20 * math.log10(max(rms, 1e-12)), 2),
                       "bytes": path.stat().st_size})
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
