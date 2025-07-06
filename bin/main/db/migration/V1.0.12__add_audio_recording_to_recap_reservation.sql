ALTER TABLE recap_reservation
ADD COLUMN interview_audio_recording_id UUID NULL,
ADD CONSTRAINT fk_recap_reservation_audio_recording_id FOREIGN KEY (interview_audio_recording_id) REFERENCES interview_audio_recording (id);
