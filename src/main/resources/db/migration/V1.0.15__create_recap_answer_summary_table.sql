CREATE TABLE recap_answer_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recap_result_id UUID NOT NULL,
    scene_id UUID NOT NULL,
    summary TEXT NOT NULL,
    scene_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recap_result
        FOREIGN KEY(recap_result_id)
        REFERENCES recap_result(id)
);

CREATE INDEX idx_recap_answer_summary_on_result_id_and_order
ON recap_answer_summary (recap_result_id, scene_order);
