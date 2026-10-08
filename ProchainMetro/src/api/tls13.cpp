#include "tls13.h"
#include <wolfssl.h>
#include <wolfssl/ssl.h>

#ifndef HAVE_SNI
#error "wolfSSL doit etre compile avec HAVE_SNI : voir ProchainMetro/src/api/README.md"
#endif

#define DELAI_MS 10000

static WOLFSSL_CTX* contexte = nullptr;

// wolfSSL ne connait pas le reseau Arduino : on lui donne de quoi lire et ecrire sur le socket
static int recevoir(WOLFSSL*, char* donnees, int taille, void* ctx) {
  WiFiClient* socket = (WiFiClient*)ctx;
  unsigned long depart = millis();
  while (!socket->available()) {
    if (!socket->connected()) return WOLFSSL_CBIO_ERR_CONN_CLOSE;
    if (millis() - depart > DELAI_MS) return WOLFSSL_CBIO_ERR_TIMEOUT;
    delay(1);
  }
  return socket->read((uint8_t*)donnees, taille);
}

static int envoyer(WOLFSSL*, char* donnees, int taille, void* ctx) {
  int n = ((WiFiClient*)ctx)->write((const uint8_t*)donnees, taille);
  return n > 0 ? n : WOLFSSL_CBIO_ERR_CONN_CLOSE;
}

bool Tls13::ouvrir(const char* hote, uint16_t port) {
  fermer();
  if (!contexte) {
    wolfSSL_Init();
    contexte = wolfSSL_CTX_new(wolfTLSv1_3_client_method());
    wolfSSL_CTX_set_verify(contexte, WOLFSSL_VERIFY_NONE, nullptr);
    wolfSSL_SetIORecv(contexte, recevoir);
    wolfSSL_SetIOSend(contexte, envoyer);
  }

  if (!socket.connect(hote, port, DELAI_MS)) {
    Serial.printf("[TLS] %s : connexion impossible\n", hote);
    return false;
  }
  ssl = wolfSSL_new(contexte);
  wolfSSL_SetIOReadCtx(ssl, &socket);
  wolfSSL_SetIOWriteCtx(ssl, &socket);
  wolfSSL_UseSNI(ssl, WOLFSSL_SNI_HOST_NAME, hote, strlen(hote));  // Cloudflare l'exige

  if (wolfSSL_connect(ssl) != WOLFSSL_SUCCESS) {
    char erreur[80];
    wolfSSL_ERR_error_string_n(wolfSSL_get_error(ssl, 0), erreur, sizeof(erreur));
    Serial.printf("[TLS] %s : echec de la negociation (%s)\n", hote, erreur);
    fermer();
    return false;
  }
  return true;
}

void Tls13::fermer() {
  if (ssl) {
    wolfSSL_shutdown(ssl);
    wolfSSL_free(ssl);
    ssl = nullptr;
  }
  socket.stop();
  debut = fin = 0;
}

// Dechiffre le paquet suivant dans le tampon. Attend jusqu'a DELAI_MS.
bool Tls13::remplir() {
  if (!ssl) return false;
  int n = wolfSSL_read(ssl, tampon, sizeof(tampon));
  if (n <= 0) return false;
  debut = 0;
  fin = n;
  return true;
}

int Tls13::available() {
  if (debut < fin) return fin - debut;
  if (!ssl) return 0;
  if (wolfSSL_pending(ssl) > 0 || socket.available()) return remplir() ? fin - debut : 0;
  return 0;
}

int Tls13::read() {
  if (debut >= fin && !remplir()) return -1;
  return tampon[debut++];
}

int Tls13::peek() {
  if (debut >= fin && !remplir()) return -1;
  return tampon[debut];
}

size_t Tls13::write(const uint8_t* donnees, size_t taille) {
  if (!ssl) return 0;
  int n = wolfSSL_write(ssl, donnees, taille);
  return n > 0 ? n : 0;
}
