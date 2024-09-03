
INSERT INTO `user`
  (`id`, `login`, `admin`, `fullname`, `affiliation`, `email`, `password_sha`, `password_reset_token`, `comment`, `created`, `lastmodified`, `status`) 
VALUES
-- TODO: remove the inner SHA1 function for the password after removing password sha1 hashing via Javascript on client
  (null,  'admin', true, 'PURL Administrator', 'My Institution', 'admin@my-institution.org', CONCAT('{SHA-256}', SHA2(SHA1('admin'),256)), null, 'main user for PURL server', NOW(3), NOW(3), 'CREATED')
ON DUPLICATE KEY UPDATE lastmodified = NOW(3), status = 'MODIFIED';
