#pragma once
#include <stdint.h>
#include <math.h>

namespace momo_protocolo {
inline bool converterCapacitancia(uint16_t msb, uint16_t lsb, double &valorPf) {
    const int32_t raw24 = static_cast<int32_t>(static_cast<int16_t>(msb)) * 256
        + static_cast<int32_t>((lsb >> 8) & 0xFF);
    if (raw24 <= -8388608 || raw24 >= 8388607) return false;
    valorPf = static_cast<double>(raw24) / 524288.0;
    return isfinite(valorPf);
}
inline bool comandoRecente(int64_t timestamp, int64_t agoraMs) {
    return timestamp > 0 && agoraMs > 1700000000000LL &&
        timestamp <= agoraMs + 30000LL && timestamp >= agoraMs - 120000LL;
}
}
