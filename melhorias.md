# Melhorias Pendentes — Deslize pra Limpar

> Gerado em 25/10/2026. Prioridades alta (segurança) e README já foram aplicadas.

---

## 🟡 Fase 2 — Qualidade de Código

### 2.1 Extrair constantes de strings mágicas

As strings `"trashed"`, `"kept"`, `"video"`, `"image"` aparecem espalhadas por vários arquivos. Criar um object de constantes:

```kotlin
// Sugestão: criar em model/Constants.kt ou data/Constants.kt
object MediaStatus {
    const val TRASHED = "trashed"
    const val KEPT = "kept"
}

object MediaType {
    const val IMAGE = "image"
    const val VIDEO = "video"
}
```

**Arquivos afetados:**
- `SwipeFragment.kt` (L515, L516, L541, L542, L661, L694, L862, L908)
- `TrashFragment.kt` (L144, L170-L177)
- `KeptAdapter.kt` (L49-L53, L64)
- `TrashAdapter.kt` (L46-L50)
- `TrashCleanupWorker.kt` (L46-L53)
- `MediaItemEntity.kt` (L17)

### 2.2 Centralizar keys de SharedPreferences

Chaves como `"deslize_prefs"`, `"tutorial_shown"`, `"permission_requested"`, `"total_space_freed"` são strings repetidas em vários locais.

```kotlin
// Sugestão: criar PrefsKeys.kt
object PrefsKeys {
    const val PREFS_NAME = "deslize_prefs"
    const val TUTORIAL_SHOWN = "tutorial_shown"
    const val PERMISSION_REQUESTED = "permission_requested"
    const val TOTAL_SPACE_FREED = "total_space_freed"
}
```

**Arquivos afetados:**
- `MainActivity.kt` (L268, L466-L467, L478)
- `SwipeFragment.kt` (L784-L786)
- `TrashFragment.kt` (L64-L66, L223-L225)
- `TrashCleanupWorker.kt` (L60-L62)

### 2.3 Extrair função utilitária para build de content URIs

A lógica `if (mediaType == "video") ContentUris.withAppendedId(Video.URI, id) else ContentUris.withAppendedId(Images.URI, id)` aparece em **5 locais**. Criar extensão:

```kotlin
// Sugestão: em model/MediaItem.kt ou utils/MediaUtils.kt
fun buildMediaUri(mediaId: Long, mediaType: String): Uri {
    val collection = if (mediaType == MediaType.VIDEO) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    return ContentUris.withAppendedId(collection, mediaId)
}
```

**Arquivos afetados:**
- `KeptAdapter.kt` (L49-L53)
- `TrashAdapter.kt` (L46-L50)
- `TrashFragment.kt` (L170-L178, L206-L214)
- `TrashCleanupWorker.kt` (L46-L53)

### 2.4 Remover OkHttp (dependência desnecessária)

O app carrega apenas conteúdo local (content URIs). OkHttp e `okhttp3-integration` são desnecessários.

**Ação:**
1. Remover de `android/app/build.gradle`:
   ```
   implementation 'com.squareup.okhttp3:okhttp:4.12.0'
   implementation 'com.github.bumptech.glide:okhttp3-integration:5.0.9'
   ```
2. Simplificar `MyAppGlideModule.kt` — remover o `registerComponents()` ou deletar o arquivo inteiro se não tiver outra config.

### 2.5 Corrigir `context?.getString()` redundante

Em `SwipeFragment.kt` linhas 858 e 905, `context` já foi obtido via `requireContext()` na L826 e não pode ser null. Trocar `context?.getString()` por `context.getString()`.

---

## 🟢 Fase 3 — Arquitetura

### 3.1 Adicionar ViewModel ao SwipeFragment

O `SwipeFragment.kt` tem 926 linhas e perde todo estado em rotação de tela. Criar um `SwipeViewModel`:

```kotlin
class SwipeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    
    val mediaItems = MutableLiveData<List<MediaItem>>()
    val currentIndex = MutableLiveData(0)
    val isRandomMode = MutableLiveData(false)
    val isSortBySizeMode = MutableLiveData(false)
    val lastAction = MutableLiveData<UndoAction?>()
    
    fun loadMedia() { /* mover queryMediaStore() e filtragem pra cá */ }
    fun markAsDeleted(item: MediaItem) { /* mover lógica */ }
    fun markAsKept(item: MediaItem) { /* mover lógica */ }
    fun performUndo() { /* mover lógica */ }
}
```

**Benefícios:**
- Estado preservado em rotação
- Fragment fica apenas com lógica de UI e gestos
- Testável unitariamente

### 3.2 Criar MediaRepository

Separar o acesso ao MediaStore do Fragment:

```kotlin
class MediaRepository(private val context: Context, private val dao: MediaDao) {
    suspend fun getUnreviewedMedia(): List<MediaItem> {
        val reviewedIds = dao.getReviewedIds().toSet()
        val allMedia = queryMediaStore()
        return allMedia.filter { it.id !in reviewedIds }
    }
    
    private fun queryMediaStore(): List<MediaItem> {
        // Mover toda a lógica de SwipeFragment.queryMediaStore() pra cá
    }
}
```

### 3.3 Considerar Paging 3 para galerias grandes

Atualmente `getReviewedIds()` carrega TODOS os IDs para memória, e `queryMediaStore()` carrega TODOS os itens. Para galerias com 50k+ itens, isso pode causar problemas de memória.

**Opções:**
- Usar `NOT IN` query diretamente no ContentResolver (mais complexo)
- Implementar Paging 3 com `PagingSource` customizado para o MediaStore
- No mínimo, paginar em blocos de 500-1000 itens

### 3.4 Implementar migrações de banco

Atualmente em `AppDatabase.kt`:
```kotlin
.fallbackToDestructiveMigration(dropAllTables = true)
```

Isso significa que qualquer mudança de schema **apaga todo o banco**, perdendo o histórico do usuário. Implementar migrações adequadas:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN nova_coluna TEXT DEFAULT ''")
    }
}

Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

---

## ⚪ Fase 5 — Features Futuras

### 5.1 Filtro por álbum/pasta
Permitir selecionar pastas específicas (Camera, WhatsApp, Downloads, Screenshots, etc.) para focar a limpeza.

- Adicionar query de `BUCKET_DISPLAY_NAME` no MediaStore
- Criar UI de seleção de pastas (chips ou dropdown)
- Esforço: **Médio**

### 5.2 Multi-seleção na lixeira
A `KeptFragment` já tem multi-select, mas a `TrashFragment` não. Adicionar seleção em lote para restaurar ou deletar vários itens de uma vez.

- Reaproveitar a lógica de seleção do `KeptAdapter`
- Esforço: **Baixo**

### 5.3 Pinch-to-zoom na tela de swipe
Permitir zoom com dois dedos para ver detalhes da foto antes de decidir.

- Usar `PhotoView` library ou implementar `ScaleGestureDetector`
- Cuidado para não conflitar com o swipe gesture
- Esforço: **Baixo**

### 5.4 Estatísticas de limpeza
Dashboard mostrando quanto espaço foi liberado ao longo do tempo, quantos itens revisados, etc.

- Salvar histórico de limpeza em nova tabela do Room
- Criar Fragment com gráficos simples
- Esforço: **Médio**

### 5.5 Tema claro
Opção de tema light para quem prefere. Criar `values-night/` e `values/` com paletas distintas.

- Esforço: **Baixo**

### 5.6 Widget de tela inicial
Widget mostrando espaço a liberar e atalho rápido para o app.

- Esforço: **Médio**

### 5.7 Suporte a GIFs
Detectar e reproduzir GIFs animados na tela de swipe (Glide já suporta isso nativamente).

- Esforço: **Baixo**

### 5.8 Testes

Atualmente: **zero testes**.

**Testes unitários sugeridos:**
- `MediaRepository` — verificar filtragem de itens já revisados
- `TrashCleanupWorker` — verificar lógica de expiração de 30 dias
- Constantes e funções utilitárias

**Testes de UI sugeridos:**
- Fluxo de swipe (left = trash, right = keep)
- Navegação entre Fragments
- Multi-seleção na KeptFragment

---

## Notas

- As fases podem ser executadas independentemente
- Fase 2 (qualidade) é a mais rápida e segura para fazer primeiro
- Fase 3 (arquitetura) requer mais cuidado, ideal fazer com testes
- Fase 5 (features) pode ser priorizada conforme feedback dos usuários
