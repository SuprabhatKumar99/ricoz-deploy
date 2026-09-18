-- Portal branding (spec section 7 / Should Have: "Portal branding")

ALTER TABLE tenants
    ADD COLUMN portal_title  VARCHAR(255),
    ADD COLUMN logo_url      VARCHAR(1024),
    ADD COLUMN primary_color VARCHAR(9); -- hex, e.g. #1A73E8
