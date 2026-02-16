-- 1. Preparación de Entorno
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS catalogos_mirror;
CREATE SCHEMA IF NOT EXISTS operational;

-- 2. Estructura de Catálogos
CREATE TABLE IF NOT EXISTS catalogos_mirror.tenants (
    tenant_id uuid PRIMARY KEY,
    name character varying(255) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE' 
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'LOCKED')),
    created_at timestamp DEFAULT now()
);

CREATE TABLE IF NOT EXISTS catalogos_mirror.departments (
    department_id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES catalogos_mirror.tenants(tenant_id),
    name character varying(255) NOT NULL,
    code character varying(50),
    active boolean DEFAULT true,
    CONSTRAINT uq_dept_tenant UNIQUE (tenant_id, department_id)
);

-- 3. Tabla de Documentos
CREATE TABLE IF NOT EXISTS operational.documents (
    document_id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id uuid NOT NULL REFERENCES catalogos_mirror.tenants(tenant_id),
    department_id uuid,
    file_hash character varying(64) NOT NULL,
    file_size_bytes bigint NOT NULL,
    content_type character varying(100),
    s3_object_key character varying(255) NOT NULL,
    dek_wrapped_value TEXT NOT NULL, 
    initialization_vector TEXT NOT NULL,
    kek_version_id VARCHAR(10),
    status character varying(20) NOT NULL 
        CHECK (status IN ('PENDING_STORAGE', 'RECEIVED', 'PROCESSED', 'ERROR')),
    metadata jsonb,
    client_ip inet,
    created_by character varying(100),
    created_at timestamp with time zone DEFAULT now(),
    
    CONSTRAINT fk_doc_hierarchy FOREIGN KEY (tenant_id, department_id) 
        REFERENCES catalogos_mirror.departments(tenant_id, department_id)
);

-- 4. Índices Críticos para Rendimiento y RLS
CREATE INDEX IF NOT EXISTS idx_docs_dept_filter ON operational.documents(tenant_id, department_id);
CREATE INDEX IF NOT EXISTS idx_docs_tenant_rls ON operational.documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_docs_timeline ON operational.documents(tenant_id, created_at DESC);

-- 5. Tabla de Auditoría
CREATE TABLE IF NOT EXISTS operational.audit_logs (
    audit_id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name character varying(50),
    operation character varying(20),
    row_id character varying(255),
    old_data jsonb,
    new_data jsonb,
    changed_by_user character varying(100),
    user_alias_nic character varying(100),
    client_ip inet,
    fecha_hora timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);