-- Campaign: created as DRAFT, moves to SCHEDULED once it has recipients and a future
-- send time, then PROCESSING while the simulator is working through recipients, then
-- COMPLETED once every recipient has a final delivery status.
CREATE TABLE campaign (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    sender_email  VARCHAR(255) NOT NULL,
    content       TEXT NOT NULL,
    scheduled_at  TIMESTAMPTZ NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaign_status ON campaign (status);
CREATE INDEX idx_campaign_created_at ON campaign (created_at);

-- Recipient belongs to exactly one campaign. status starts as PENDING and is flipped to
-- DELIVERED/FAILED by the send simulator when the campaign is processed.
CREATE TABLE recipient (
    id           BIGSERIAL PRIMARY KEY,
    campaign_id  BIGINT NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    name         VARCHAR(150) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recipient_campaign ON recipient (campaign_id);

-- same email can't be added twice to the same campaign
CREATE UNIQUE INDEX uq_recipient_campaign_email ON recipient (campaign_id, email);
