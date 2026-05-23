# Documentação do Banco de Dados e API do Power Connection

Esta documentação detalha toda a modelagem de dados, arquiteturas de persistência local (**Room Database**) e sincronização remota (**Supabase / PostgreSQL**), bem como as diretrizes completas de endpoints REST (CRUD) para o desenvolvimento de um back-end dedicado ou integração direta.

---

## 1. Arquitetura de Sincronização e Fluxo de Dados

O aplicativo está estruturado sobre o padrão **Offline-First**. O app consome e grava dados localmente em um banco de dados SQLite gerenciado pelo Jetpack Room e dispara chamadas assíncronas para sincronização na nuvem através das credenciais do Supabase.

```
       [ Interface Compose UI ]
                 │ (State & Flow)
                 ▼
          [ MainViewModel ]
                 │
                 ▼
        [ AppRepository ]
         /             \
        v               v
  [ Room (Local) ]   [ SupabaseSync (Remoto) ]
  (SQLite local)     (API REST HTTP - PostgreSQL)
```

- **Room Database (`AppDatabase`)**: Atua como fonte de verdade imediata na UI, garantindo latência zero e operação 100% offline.
- **Supabase REST / Back-end**: Interface HTTP REST que recebe payloads em JSON e persiste no PostgreSQL. Empregamos cabeçalhos padrão como `Prefer: resolution=merge-duplicates` para realizar ações de *upsert* (inserir ou atualizar) transparentemente baseando-se nas chaves primárias.

---

## 2. Estrutura de Tabelas (DML / DDL SQL) - PostgreSQL Sólido

Abaixo estão os scripts de criação de tabelas (**DDL**) compatíveis com PostgreSQL (Supabase) para a estruturação do banco de dados relacional remoto:

```sql
-- Habilita extensão de UUID caso queira migrar chaves futuras para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. PROFILES (Perfis de Usuários)
CREATE TABLE IF NOT EXISTS "profiles" (
    "id" VARCHAR(255) PRIMARY KEY, -- Email do usuário / ID único
    "nome" VARCHAR(255) NOT NULL,
    "curso" VARCHAR(255) NOT NULL DEFAULT 'Pedagogia (UERJ)',
    "periodo" VARCHAR(100) NOT NULL,
    "bio" TEXT DEFAULT '',
    "foto_url" TEXT DEFAULT '',
    "selected_materia" VARCHAR(255) DEFAULT '',
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 2. ROLES (Papéis e Permissões Administrativas)
CREATE TABLE IF NOT EXISTS "roles" (
    "user_id" VARCHAR(255) PRIMARY KEY REFERENCES "profiles"("id") ON DELETE CASCADE,
    "role" VARCHAR(50) NOT NULL DEFAULT 'aluno', -- super_admin, moderador, aluno
    "permissions" TEXT DEFAULT 'delete_posts,manage_events,moderate_chats', -- custom csv permissions
    "principal_area" VARCHAR(255) DEFAULT 'Pedagogia (UERJ)',
    "online" BOOLEAN DEFAULT true
);

-- 3. SUBJECTS (Disciplinas / Matérias)
CREATE TABLE IF NOT EXISTS "subjects" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT ''
);

-- 4. POSTS (Feed Compartilhado)
CREATE TABLE IF NOT EXISTS "posts" (
    "id" SERIAL PRIMARY KEY,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_avatar" TEXT DEFAULT '',
    "subject_id" INT REFERENCES "subjects"("id") ON DELETE SET NULL,
    "content" TEXT NOT NULL,
    "media_url" TEXT DEFAULT NULL, -- Link para imagem ou anexo do Storage
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT,
    "likes_count" INT NOT NULL DEFAULT 0
);

-- 5. COMMENTS (Comentários nos posts do Feed)
CREATE TABLE IF NOT EXISTS "comments" (
    "id" SERIAL PRIMARY KEY,
    "post_id" INT NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_avatar" TEXT DEFAULT '',
    "content" TEXT NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 6. POST_LIKES (Controle de Likes únicos de posts)
CREATE TABLE IF NOT EXISTS "post_likes" (
    "post_id" INT NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    PRIMARY KEY ("post_id", "user_id")
);

-- 7. MINDMAPS (Mapas Mentais compartilhados pelos alunos)
CREATE TABLE IF NOT EXISTS "mindmaps" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "image_url" TEXT NOT NULL, -- URL do bucket 'mindmaps' do Storage
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "tags" VARCHAR(255) DEFAULT '', -- Separadas por vírgula
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 8. MINDMAP_FAVORITES (Controle de curtidas/favoritos em Mapas Mentais)
CREATE TABLE IF NOT EXISTS "mindmap_favorites" (
    "mindmap_id" INT NOT NULL REFERENCES "mindmaps"("id") ON DELETE CASCADE,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    PRIMARY KEY ("mindmap_id", "user_id")
);

-- 9. HELP_REQUESTS (Dúvidas Acadêmicas / Mural 'Preciso de Ajuda')
CREATE TABLE IF NOT EXISTS "help_requests" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT NOT NULL,
    "subject_id" INT NOT NULL REFERENCES "subjects"("id") ON DELETE CASCADE,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "is_resolved" BOOLEAN NOT NULL DEFAULT false,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 10. MESSAGES (Mensagens do Chat Colaborativo)
CREATE TABLE IF NOT EXISTS "messages" (
    "id" SERIAL PRIMARY KEY,
    "content" TEXT NOT NULL,
    "media_url" TEXT DEFAULT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_role" VARCHAR(100) NOT NULL DEFAULT 'aluno', -- Administrador, Moderador, Aluno
    "room_tag" VARCHAR(100) DEFAULT NULL, -- Tag do canal de chat (ex: 'geral', 'psicologia')
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 11. NOTIFICATIONS (Notificações aos usuários)
CREATE TABLE IF NOT EXISTS "notifications" (
    "id" SERIAL PRIMARY KEY,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "title" VARCHAR(255) NOT NULL,
    "body" TEXT NOT NULL,
    "read" BOOLEAN NOT NULL DEFAULT false,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 12. STUDY_GROUPS (Grupos de Estudo)
CREATE TABLE IF NOT EXISTS "study_groups" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "created_by" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 13. GROUP_THREADS (Fóruns/Discussões internos de um Grupo de Estudo)
CREATE TABLE IF NOT EXISTS "group_threads" (
    "id" SERIAL PRIMARY KEY,
    "group_id" INT NOT NULL REFERENCES "study_groups"("id") ON DELETE CASCADE,
    "title" VARCHAR(255) NOT NULL,
    "content" TEXT NOT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 14. GROUP_COMMENTS (Comentários nos tópicos de discussão do fórum)
CREATE TABLE IF NOT EXISTS "group_comments" (
    "id" SERIAL PRIMARY KEY,
    "thread_id" INT NOT NULL REFERENCES "group_threads"("id") ON DELETE CASCADE,
    "content" TEXT NOT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 15. CALENDAR_EVENTS (Agenda Acadêmica compartilhada / Cronograma)
CREATE TABLE IF NOT EXISTS "calendar_events" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "date" BIGINT NOT NULL, -- Timestamp da data do evento
    "category" VARCHAR(100) NOT NULL, -- 'avaliação', 'aviso', 'evento'
    "created_by" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE
);

-- 16. AUDIT_LOGS (Log de ações administrativas/moderações)
CREATE TABLE IF NOT EXISTS "audit_logs" (
    "id" SERIAL PRIMARY KEY,
    "moderator_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "moderator_name" VARCHAR(255) NOT NULL,
    "action" VARCHAR(255) NOT NULL, -- ex: "Post Deletado", "Evento Criado", "Material Excluído"
    "target" TEXT NOT NULL,
    "timestamp" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 17. STUDY_MATERIALS (Materiais de Apoio - Biblioteca)
CREATE TABLE IF NOT EXISTS "study_materials" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "file_url" TEXT NOT NULL, -- URL do arquivo no bucket 'materials'
    "type" VARCHAR(100) NOT NULL DEFAULT 'material', -- 'material', 'prova', 'audiobook'
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);
```

---

## 3. Estrutura de Armazenamento de Arquivos (Storage Buckets)

Para que o CRUD e o aplicativo salvem mídias e arquivos corretamente no Storage do Supabase (ou em qualquer back-end com armazenamento em nuvem), devem ser mapeados **dois buckets públicos**:

1. **`mindmaps`**:
   - **Objetivo**: Armazenar os arquivos de imagem dos mapas mentais enviados pelos alunos.
   - **Tipos de Arquivos Suportados**: PNG, JPG, JPEG (limite recomendado de 10 MB).
   - **Estrutura de URL esperada pelo App**:
     `https://<sua_url_supabase>/storage/v1/object/public/mindmaps/<nome_do_arquivo.png>`

2. **`materials`**:
   - **Objetivo**: Armazenar os arquivos de biblioteca acadêmica, incluindo propostas de avaliações passadas (PDFs), resumos de matérias acadêmicas e podcasts/audiobooks.
   - **Tipos de Arquivos Suportados**: PDF, MP3, DOCX, PPTX (limite recomendado de 25 MB).
   - **Estrutura de URL esperada pelo App**:
     `https://<sua_url_supabase>/storage/v1/object/public/materials/<nome_do_arquivo.pdf>`

---

## 4. Especificação de Endpoints REST para CRUD (Criação de Back-end Dedicado)

Caso você opte por construir um back-end intermediário (em Node.js, Python, Go etc.) para expor uma API padrão ao aplicativo em vez de expor o Supabase direto, abaixo estão os endpoints ideais com suporte a todas as operações de **CRUD** do app.

### Autenticação & Cabeçalhos Universais
Os cabeçalhos a seguir devem ser providenciados pelo cliente REST ao realizar as operações no back-end (emulando as chamadas efetuadas pelo OkHttpClient interno do app):

```http
apikey: ENVS_SECRET_OAUTH_TOKEN_OR_SUPABASE_ANON
Authorization: Bearer <TOKEN_DE_AUTENTICACAO_OU_ANON>
Content-Type: application/json
```

---

### CRUD 1: Perfis & Onboarding (`/profiles`)
O onboarding do app atualiza ou cria o registro do estudante usando a estratégia de **Upsert**.

* **`POST /rest/v1/profiles`**: Insere ou Atualiza um Perfil.
  * **Cabeçalho Prefer**: `Prefer: resolution=merge-duplicates`
  * **Payload esperado**:
    ```json
    {
      "id": "usuario@exemplo.com",
      "nome": "João da Silva",
      "curso": "Pedagogia (UERJ)",
      "periodo": "1º Período",
      "bio": "Estudante apaixonado por tecnologia na educação.",
      "foto_url": "https://gravatar.com/...",
      "created_at": 1716432000000
    }
    ```
* **`GET /rest/v1/profiles?id=eq.{email}`**: Obtém o perfil de um usuário específico.

---

### CRUD 2: Feed & Posts (`/posts`)
Adição, deleção e listagem de postagens acadêmicas ou avisos de repúdio/estudo.

* **`GET /rest/v1/posts?order=created_at.desc`**: Lista posts ordenados por data decrescente.
* **`POST /rest/v1/posts`**: Cria um novo post no mural.
  * **Payload esperado**:
    ```json
    {
      "author_id": "usuario@exemplo.com",
      "author_name": "João da Silva",
      "author_avatar": "https://...",
      "content": "Alguém estudando Didática Geral hoje à noite?",
      "media_url": null,
      "created_at": 1716432000000
    }
    ```
* **`DELETE /rest/v1/posts?id=eq.{postId}`**: Deleta um post (ação reservada para o autor ou moderador administrativo).

---

### CRUD 3: Mapas Mentais (`/mindmaps`)
Compartilhamento e consulta de links e imagens de mapas dinâmicos construídos pelos alunos.

* **`GET /rest/v1/mindmaps`**: Obtém todos os mapas mentais criados.
* **`POST /rest/v1/mindmaps`**: Publica um novo mapa mental.
  * **Payload esperado**:
    ```json
    {
      "title": "Teoria Cognitiva de Piaget",
      "description": "Foco nos estágios de desenvolvimento infantil.",
      "image_url": "https://supabase.uerj.edu/storage/v1/object/public/mindmaps/piaget_map.png",
      "author_id": "professor@uerj.br",
      "author_name": "Profa. Maria Silva",
      "tags": "Piaget, Didática, Estudo",
      "created_at": 1716432050000
    }
    ```
* **`DELETE /rest/v1/mindmaps?id=eq.{mapId}`**: Remove um mapa mental cadastrado.

---

### CRUD 4: Biblioteca de Materiais (`/study_materials`)
Permite aos alunos adicionarem resumos de provas antigas, questões simuladas ou podcasts em MP3 de matérias.

* **`GET /rest/v1/study_materials`**: Retorna livros, PDFs de provas, e MP3s.
* **`POST /rest/v1/study_materials`**: Cadastra um novo material de apoio no sistema.
  * **Payload esperado**:
    ```json
    {
      "title": "Simulado de Psicologia da Educação",
      "description": "Prova resolvida do Período de 2023.2.",
      "file_url": "https://supabase.uerj.edu/storage/v1/object/public/materials/simulado_psico_2023.pdf",
      "type": "prova",
      "author_id": "aluno@uerj.br",
      "author_name": "Rodrigo Costa",
      "created_at": 1716432100000
    }
    ```
* **`DELETE /rest/v1/study_materials?id=eq.{materialId}`**: Exclui o material de apoio.

---

### CRUD 5: Grupos e Discussões (`/study_groups` / `/group_threads`)
Gestão de grupos e salas virtuais construídas espontaneamente.

* **`POST /rest/v1/study_groups`**: Cria um novo grupo de estudos.
  ```json
  {
    "name": "Grupo de Estudos Vygotsky",
    "description": "Focado no estudo socio-histórico do desenvolvimento infantil.",
    "created_by": "aluno@uerj.br"
  }
  ```
* **`POST /rest/v1/group_threads`**: Cria um tópico de discussão dentro do fórum do grupo de estudos.
  ```json
  {
    "group_id": 1,
    "title": "Interacionismo vs Cognitivismo",
    "content": "Qual a principal diferença prática para a pedagogia aplicada?",
    "author_id": "aluno@uerj.br",
    "author_name": "Rodrigo Costa"
  }
  ```

---

## 5. Exemplo Rápido de Implementação do Back-end (Exemplo em Node.js & TypeScript)

Estes controladores Express servem como exemplo para você estruturar o back-end usando ferramentas tradicionais (como Prisma ORM ou conexão postgres direta):

```typescript
import express, { Request, Response } from 'express';
import { Pool } from 'pg';

const router = express.Router();
const pool = new Pool({
  connectionString: process.env.DATABASE_URL, // Conexão direta postgres
});

// GET /rest/v1/posts
router.get('/posts', async (req: Request, res: Response) => {
  try {
    const result = await pool.query('SELECT * FROM posts ORDER BY created_at DESC');
    return res.status(200).json(result.rows);
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
});

// POST /rest/v1/posts
router.post('/posts', async (req: Request, res: Response) => {
  const { author_id, author_name, author_avatar, content, media_url, created_at } = req.body;
  try {
    const query = `
      INSERT INTO posts (author_id, author_name, author_avatar, content, media_url, created_at)
      VALUES ($1, $2, $3, $4, $5, COALESCE($6, extract(epoch from now()) * 1000))
      RETURNING *;
    `;
    const result = await pool.query(query, [author_id, author_name, author_avatar, content, media_url, created_at]);
    return res.status(201).json(result.rows[0]);
  } catch (error: any) {
    return res.status(400).json({ error: error.message });
  }
});

// POST /rest/v1/profiles (Upsert utilizando o padrão PostgreSQL ON CONFLICT)
router.post('/profiles', async (req: Request, res: Response) => {
  const { id, nome, curso, periodo, bio, foto_url } = req.body;
  try {
    const query = `
      INSERT INTO profiles (id, nome, curso, periodo, bio, foto_url)
      VALUES ($1, $2, $3, $4, $5, $6)
      ON CONFLICT (id) DO UPDATE SET
        nome = EXCLUDED.nome,
        curso = EXCLUDED.curso,
        periodo = EXCLUDED.periodo,
        bio = EXCLUDED.bio,
        foto_url = EXCLUDED.foto_url
      RETURNING *;
    `;
    const result = await pool.query(query, [id, nome, curso, periodo, bio, foto_url]);
    return res.status(200).json(result.rows[0]);
  } catch (error: any) {
    return res.status(400).json({ error: error.message });
  }
});

export default router;
```

---

## 6. Recursos Acadêmicos e Segurança RLS (Row Level Security)

Se você estiver usando o Supabase direto, sugerimos criar as regras de RLS (Segurança a Nível de Linhas) na tabela `roles` e `audit_logs` para evitar alterações de usuários comuns:

```sql
-- Ativa segurança nas tabelas sensíveis
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Cria política permitindo inserção por qualquer um, mas leitura livre ou restrita apenas a moderadores
CREATE POLICY "Apenas moderadores leem logs de auditoria" ON audit_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM roles 
            WHERE roles.user_id = auth.jwt() ->> 'email' 
            AND roles.role IN ('super_admin', 'moderador')
        )
    );
```

---
Esta especificação serve como matriz estrutural e modelo de dados para que você possa escalar a sincronização e desenvolver integrações ricas do Power Connection com qualquer outro back-end acadêmico institucional.
