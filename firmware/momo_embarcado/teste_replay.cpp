#ifndef ARDUINO
// Compilar no PC: g++ -std=c++17 -O2 teste_replay.cpp -o teste_replay
// Entrada: tempo_ms,capacitancia_pf (apenas linhas reais, sem Marker).
#include <iostream>
#include "momo_inferencia.h"

int main() {
  momo::Inferencia algoritmo(0);
  uint32_t tempo;
  double cap;
  char virgula;
  while (std::cin >> tempo >> virgula >> cap) {
    if (virgula != ',') return 2;
    momo::Classe classe = momo::URINA;
    if (algoritmo.processar(tempo, cap, classe)) {
      std::cout << tempo << ',' << momo::nomeClasse(classe) << '\n';
    }
  }
  return 0;
}

#endif  // ARDUINO: este programa main() e exclusivo do PC.
