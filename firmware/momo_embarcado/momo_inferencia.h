#pragma once

// Núcleo independente do Arduino: recebe uma capacitância e um instante.
// NÃO filtra, suaviza, interpola ou usa dados futuros.
#include <stdint.h>
#include <math.h>
#include "momo_modelo_treinado.h"

namespace momo {

enum Classe : uint8_t { DIARREIA = 0, FEZES = 1, URINA = 2 };

inline const char *nomeClasse(Classe c) {
  switch (c) {
    case URINA: return "urina";
    case FEZES: return "fezes";
    default: return "diarreia";
  }
}

inline double distanciaDtw(int indiceTemplate, const double *prefixo) {
  const int k = momo_modelo::K_PREFIXO;
  double anterior[momo_modelo::K_PREFIXO + 1];
  double atual[momo_modelo::K_PREFIXO + 1];
  const double infinito = INFINITY;
  for (int j = 0; j <= k; ++j) anterior[j] = infinito;
  anterior[0] = 0.0;

  for (int i = 1; i <= k; ++i) {
    atual[0] = infinito;
    for (int j = 1; j <= k; ++j) {
      const double erro = prefixo[i - 1] - momo_modelo::ASSINATURAS[indiceTemplate][j - 1];
      double minimo = anterior[j];
      if (atual[j - 1] < minimo) minimo = atual[j - 1];
      if (anterior[j - 1] < minimo) minimo = anterior[j - 1];
      atual[j] = erro * erro + minimo;
    }
    for (int j = 0; j <= k; ++j) anterior[j] = atual[j];
  }
  return anterior[k] / k;
}

inline Classe classificarPrefixo(const double *prefixo) {
  double menor = INFINITY;
  uint8_t melhorClasse = DIARREIA;
  for (uint8_t i = 0; i < momo_modelo::TOTAL_TEMPLATES; ++i) {
    const double d = distanciaDtw(i, prefixo);
    const uint8_t classe = momo_modelo::CLASSE_TEMPLATE[i];
    if (d < menor || (d == menor && classe < melhorClasse)) {
      menor = d;
      melhorClasse = classe;
    }
  }
  return static_cast<Classe>(melhorClasse);
}

inline bool eventoRf(const float x[3]) {
  double probabilidadeTotal = 0.0;
  for (uint8_t arvore = 0; arvore < momo_modelo::TOTAL_ARVORES; ++arvore) {
    uint16_t indice = momo_modelo::RAIZES[arvore];
    while (true) {
      const momo_modelo::NoRF &no = momo_modelo::NOS[indice];
      if (no.atributo < 0) {
        probabilidadeTotal += no.p_evento;
        break;
      }
      indice = (x[no.atributo] <= no.limiar) ? no.esquerdo : no.direito;
    }
  }
  // O sklearn escolhe a classe 0 em empate exato.
  return probabilidadeTotal > (momo_modelo::TOTAL_ARVORES / 2.0);
}

class Inferencia {
 public:
  enum Estado : uint8_t { CALIBRANDO, MONITORANDO, COLETANDO_PREFIXO, CLASSIFICADO };

  explicit Inferencia(uint32_t instanteInicio = 0) { reiniciar(instanteInicio); }

  void reiniciar(uint32_t instanteInicio) {
    inicioMs_ = instanteInicio;
    estado_ = CALIBRANDO;
    somaSeca_ = 0.0;
    nSecas_ = 0;
    c0_ = 0.0;
    anteriorC_ = 0.0;
    anteriorD1_ = 0.0;
    temAnterior_ = false;
    nPrefixo_ = 0;
  }

  // Retorna true apenas quando uma classe acaba de ser identificada.
  bool processar(uint32_t instanteMs, double capacitanciaPf, Classe &resultado) {
    if (!isfinite(capacitanciaPf)) return false;
    const double d1 = temAnterior_ ? capacitanciaPf - anteriorC_ : 0.0;
    const double d2 = temAnterior_ ? d1 - anteriorD1_ : 0.0;
    anteriorC_ = capacitanciaPf;
    anteriorD1_ = d1;
    temAnterior_ = true;

    if (estado_ == CLASSIFICADO) return false;
    if (estado_ == CALIBRANDO) {
      if (static_cast<uint32_t>(instanteMs - inicioMs_) < momo_modelo::CALIBRACAO_MS) {
        somaSeca_ += capacitanciaPf;
        ++nSecas_;
        return false;
      }
      // Leitura na fronteira da calibração NÃO compõe a média seca.
      if (nSecas_ == 0) return false;
      c0_ = somaSeca_ / nSecas_;
      estado_ = MONITORANDO;
    }

    const double delta = capacitanciaPf - c0_;
    const float atributos[3] = {
      static_cast<float>(delta), static_cast<float>(d1), static_cast<float>(d2)
    };

    if (estado_ == MONITORANDO && eventoRf(atributos)) {
      estado_ = COLETANDO_PREFIXO;
    }
    if (estado_ == COLETANDO_PREFIXO) {
      prefixo_[nPrefixo_++] = delta;
      if (nPrefixo_ == momo_modelo::K_PREFIXO) {
        resultado = classificarPrefixo(prefixo_);
        estado_ = CLASSIFICADO;
        return true;
      }
    }
    return false;
  }

  Estado estado() const { return estado_; }
  double referenciaSecaPf() const { return c0_; }
  uint32_t nLeiturasSecas() const { return nSecas_; }
  uint8_t prefixoAtual() const { return nPrefixo_; }

 private:
  uint32_t inicioMs_ = 0;
  Estado estado_ = CALIBRANDO;
  double somaSeca_ = 0.0, c0_ = 0.0;
  uint32_t nSecas_ = 0;
  double anteriorC_ = 0.0, anteriorD1_ = 0.0;
  bool temAnterior_ = false;
  double prefixo_[momo_modelo::K_PREFIXO] = {};
  uint8_t nPrefixo_ = 0;
};

}  // namespace momo
