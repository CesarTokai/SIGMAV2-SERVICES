-- Permite que id_label_request sea NULL en labels_cancelled
-- para soportar marbetes históricos que no tienen solicitud de folios asociada

ALTER TABLE labels_cancelled
    MODIFY COLUMN id_label_request BIGINT NULL;

