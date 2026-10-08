#pragma once
#include <WiFiClient.h>

struct WOLFSSL;

// Connexion HTTPS en TLS 1.3, via wolfSSL.
// Le serveur de PRIM n'accepte que TLS 1.3, que le core ESP32 Arduino (mbedTLS) ne sait pas faire.
// S'utilise comme un Stream : on ecrit la requete, on lit la reponse.
// Le certificat du serveur n'est pas verifie (comme setInsecure()).
class Tls13 : public Stream {
public:
  ~Tls13() { fermer(); }

  bool ouvrir(const char* hote, uint16_t port = 443);
  void fermer();

  int available() override;
  int read() override;
  int peek() override;
  size_t write(uint8_t octet) override { return write(&octet, 1); }
  size_t write(const uint8_t* donnees, size_t taille) override;

private:
  bool remplir();

  WiFiClient socket;
  WOLFSSL* ssl = nullptr;
  uint8_t tampon[512];
  int debut = 0, fin = 0;
};
