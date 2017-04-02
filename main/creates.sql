
CREATE DATABASE IF NOT EXISTS mac_vendor;
USE mac_vendor;

DROP TABLE IF EXISTS vendors;
CREATE TABLE vendors
(
	mac				VARCHAR(8) NOT NULL,
	vendor			VARCHAR(512),
	vendor_group	VARCHAR(512),
	PRIMARY KEY(mac)
);


