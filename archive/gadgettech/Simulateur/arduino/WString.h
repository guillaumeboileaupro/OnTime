#pragma once
#include <string>
#include <string.h>

// Le String d'Arduino, juste ce qu'utilise ProchainMetro, par-dessus std::string
class String {
public:
  String(const char* texte = "") : s(texte) {}
  unsigned int length() const { return s.size(); }
  const char* c_str() const { return s.c_str(); }
  void remove(unsigned int debut) { if (debut < s.size()) s.erase(debut); }
  void trim() {
    size_t debut = s.find_first_not_of(" \t\r\n");
    size_t fin = s.find_last_not_of(" \t\r\n");
    s = debut == std::string::npos ? "" : s.substr(debut, fin - debut + 1);
  }
  bool endsWith(const char* fin) const {
    size_t n = strlen(fin);
    return s.size() >= n && s.compare(s.size() - n, n, fin) == 0;
  }
  String operator+(const char* autre) const { return String((s + autre).c_str()); }

private:
  std::string s;
};
