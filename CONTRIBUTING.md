# Guia de Contribuição — Deslize pra Limpar

Obrigado pelo interesse em contribuir com o **Deslize pra Limpar**! Este documento orienta sobre o fluxo de desenvolvimento, padrões de código e envio de pull requests.

---

## 🛠️ Configuração do Ambiente

1. **JDK**: Certifique-se de ter o JDK 17 ou superior configurado.
2. **Android Studio**: Recomendado usar a versão estável mais recente (Ladybug / Koala ou superior).
3. **Android SDK**: O projeto requer `compileSdk 37` e `minSdk 23`.
4. **Clone o repositório**:
   ```bash
   git clone git@github.com:lagando/Deslize-pra-limpar.git
   cd Deslize-pra-limpar
   ```
5. **Abra o projeto**: No Android Studio, abra a pasta `android/`.

---

## 📋 Padrões de Código

- **Linguagem**: 100% Kotlin.
- **Arquitetura**: MVVM (Model-View-ViewModel):
  - Lógica de UI e animações nos `Fragments` / `Activities`.
  - Estado e regras de negócio no `ViewModel` (usando `LiveData` / `StateFlow`).
  - Acesso a dados e MediaStore isolados no `MediaRepository` e DAOs do Room.
- **Constantes**:
  - Evite "strings mágicas" soltas no código. Use `MediaStatus`, `MediaType` e `PrefsKeys`.
- **URIs do MediaStore**:
  - Use sempre a extensão/utilitário `buildMediaUri(mediaId, mediaType)`.
- **Strings e Internacionalização**:
  - Nunca coloque textos fixos no código ou layouts. Adicione tanto em `res/values/strings.xml` (Inglês) quanto em `res/values-pt/strings.xml` (Português).
- **Segurança**:
  - **Nunca** commite chaves de assinatura (`.keystore`), senhas ou tokens no repositório.

---

## 🧪 Testes

Antes de submeter alterações, execute a suíte de testes unitários:

```bash
cd android
./gradlew test
```

Novas funcionalidades e utilitários devem vir acompanhados de seus respectivos testes unitários em `android/app/src/test/`.

---

## 💬 Padrão de Commits

Utilizamos o padrão de [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` Nova funcionalidade
- `fix:` Correção de bug
- `refactor:` Alteração que melhora a estrutura do código sem alterar comportamento
- `test:` Inclusão ou ajuste de testes
- `docs:` Alterações na documentação
- `chore:` Tarefas de manutenção (build, gitignore, etc.)

---

## 🚀 Como Enviar um Pull Request

1. Crie uma branch com o nome da sua feature:
   ```bash
   git checkout -b feature/minha-melhoria
   ```
2. Faça seus commits de forma atômica e descritiva.
3. Certifique-se de que os testes passam e o build compila sem erros.
4. Envie sua branch para o GitHub:
   ```bash
   git push origin feature/minha-melhoria
   ```
5. Abra um **Pull Request** explicando claramente o que foi feito, o motivo e como testar.
