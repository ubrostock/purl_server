INSERT INTO `purl`.`user`
  (`id`, `admin`, `fullname`, `affiliation`, `email`, `login`, `password_sha`, `password_reset_token`, `comment`, `created`, `lastmodified`, `status`) 
VALUES
  (null,  true, 'PURL Administrator', 'My Institution', 'admin@my-institution.org', 'admin', SHA1('admin'), null, 'main user for PURL server', NOW(), NOW(), 'CREATED')
ON DUPLICATE KEY UPDATE lastmodified = NOW(), status = 'MODIFIED';
