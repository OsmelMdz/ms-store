-- 1. Crear esquema
CREATE SCHEMA IF NOT EXISTS operational;

-- 2. Crear tabla de documentos con soporte para Aislamiento y Rotación
CREATE TABLE IF NOT EXISTS operational.documents (
    -- Usamos UUID como PK para mayor flexibilidad que el Hash
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Aislamiento Multitenant y Departamental
    tenant_id UUID NOT NULL,
    department_id UUID NOT NULL,
    
    -- Metadatos del archivo
    file_hash VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    content_type VARCHAR(100),
    
    -- Referencia a MinIO
    s3_object_key VARCHAR(255) NOT NULL,
    
    -- CAMPOS CRÍTICOS: Envelope Encryption y Rotación de Vault
    dek_wrapped_value TEXT NOT NULL,         -- El DEK cifrado por Vault (incluye prefijo de versión)
    initialization_vector TEXT NOT NULL,    -- IV usado para el cifrado AES local
    kek_version_id VARCHAR(10),             -- Auditoría de la versión de la llave de Vault
    
    -- Trazabilidad y Estado
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_by VARCHAR(100),
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Unicidad: Un mismo archivo no debería repetirse en el mismo departamento
    CONSTRAINT unique_file_dept UNIQUE (file_hash, tenant_id, department_id)
);

-- 3. Índices para el Aislamiento (Cruciales para RLS)
CREATE INDEX IF NOT EXISTS idx_docs_tenant_dept ON operational.documents(tenant_id, department_id);
CREATE INDEX IF NOT EXISTS idx_docs_hash ON operational.documents(file_hash);

-- 4. Llaves Foráneas (Garantizan que el Tenant y Dept existan en catálogos)
ALTER TABLE operational.documents 
ADD CONSTRAINT fk_docs_tenant FOREIGN KEY (tenant_id) 
REFERENCES catalogos_mirror.tenants(tenant_id);

ALTER TABLE operational.documents 
ADD CONSTRAINT fk_docs_dept FOREIGN KEY (department_id) 
REFERENCES catalogos_mirror.departments(department_id);

-- 5. Comentarios de seguridad
COMMENT ON COLUMN operational.documents.dek_wrapped_value IS 'DEK cifrado por Vault Transit. Soporta rotación mediante prefijo vault:vX:';
COMMENT ON COLUMN operational.documents.department_id IS 'Nivel secundario de aislamiento para documentos del mismo tenant';