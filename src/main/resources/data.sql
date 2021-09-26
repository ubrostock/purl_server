INSERT INTO `user`
  (`id`, `login`, `admin`, `fullname`, `affiliation`, `email`, `password_sha`, `password_reset_token`, `comment`, `created`, `lastmodified`, `status`) 
VALUES
  (null,  'admin', true, 'PURL Administrator', 'My Institution', 'admin@my-institution.org', SHA1('admin'), null, 'main user for PURL server', NOW(), NOW(), 'CREATED')
ON DUPLICATE KEY UPDATE lastmodified = NOW(), status = 'MODIFIED';
