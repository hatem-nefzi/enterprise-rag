
-- add missing storage_path column
ALTER TABLE documents 
    ADD COLUMN storage_path VARCHAR(500);

-- fix status constraint to match DocumentStatus enum
ALTER TABLE documents 
    DROP CONSTRAINT documents_status_check;

ALTER TABLE documents 
    ADD CONSTRAINT documents_status_check 
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));

-- update default to match new enum
ALTER TABLE documents 
    ALTER COLUMN status SET DEFAULT 'PENDING';