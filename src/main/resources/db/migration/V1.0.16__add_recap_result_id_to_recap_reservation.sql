ALTER TABLE recap_reservation
ADD COLUMN recap_result_id UUID;

ALTER TABLE recap_reservation
ADD CONSTRAINT fk_recap_result
    FOREIGN KEY(recap_result_id)
    REFERENCES recap_result(id);
