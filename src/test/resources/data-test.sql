DELETE FROM `user` WHERE true;
DELETE FROM `purl` WHERE true;
DELETE FROM `purlhistory` WHERE true;
DELETE FROM `domain` WHERE true;
DELETE FROM `domainuser` WHERE true;

INSERT INTO `user`
  (`id`, `login`, `admin`, `fullname`, `affiliation`, `email`, `password_sha`, `password_reset_token`, `comment`, `created`, `lastmodified`, `status`) 
VALUES
-- TODO: remove the inner SHA1 function for the password after removing password sha1 hashing via Javascript on client
  (1,  'admin', true, 'PURL Administrator', 'My Institution', 'admin@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'admin'))), null, 'main user for PURL server', NOW(3), NOW(3), 'CREATED'),
  (101,  'user1', false, 'PURL User1', 'My Institution', 'user1@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'user1'))), null, 'user1 for PURL server', NOW(3), NOW(3), 'CREATED'),
  (102,  'user2', false, 'PURL User2', 'My Institution', 'user1@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'user2'))), null, 'user2 for PURL server', NOW(3), NOW(3), 'CREATED'),
  (103,  'user3', false, 'PURL User3', 'My Institution', 'user1@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'user3'))), null, 'user3 for PURL server', NOW(3), NOW(3), 'CREATED'),
  (104,  'user4', false, 'PURL User4', 'My Institution', 'user1@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'user4'))), null, 'user4 for PURL server', NOW(3), NOW(3), 'CREATED');

INSERT INTO `domain` (`id`, `path`, `name`, `comment`, `created`, `lastmodified`, `status`) 
       VALUES (12, '/test', 'Test Domain', '', '2012-07-26 11:20:32.000', '2023-09-21 12:05:06.010', 'MODIFIED');

INSERT INTO `domain` (`id`, `path`, `name`, `comment`, `created`, `lastmodified`, `status`) 
       VALUES (13, '/deleted', 'Test Domain', '', '2012-07-26 11:20:32.000', '2023-09-21 12:05:06.010', 'DELETED');
       
INSERT INTO `domain` (`id`, `path`, `name`, `comment`, `created`, `lastmodified`, `status`) 
       VALUES (14, '/modify', 'Test Domain', '', '2012-07-26 11:20:32.000', '2023-09-21 12:05:06.010', 'MODIFIED');

INSERT INTO `domainuser` (`id`, `domain_id`, `user_id`, `can_create`, `can_modify`) 
       VALUES (null, 12, 101, true, true);

INSERT INTO `domainuser` (`id`, `domain_id`, `user_id`, `can_create`, `can_modify`) 
       VALUES (null, 13, 101, true, true);
       
INSERT INTO `domainuser` (`id`, `domain_id`, `user_id`, `can_create`, `can_modify`) 
       VALUES (null, 12, 102, false, true);
       
INSERT INTO `domainuser` (`id`, `domain_id`, `user_id`, `can_create`, `can_modify`) 
       VALUES (null, 14, 104, true, false);
           
INSERT INTO `purl` (`path`, `domain_id`, `type`, `target`, `created`, `lastmodified`, `status`)
       VALUES ('/test/test123', 12, 'PARTIAL_302', 'https://example.com', NOW(3), NOW(3), 'CREATED');
       
INSERT INTO `purl` (`path`, `domain_id`, `type`, `target`, `created`, `lastmodified`, `status`)
       VALUES ('/test/redirect1', 12, 'REDIRECT_302', 'https://example.com', NOW(3), NOW(3), 'CREATED');
       
INSERT INTO `purl` (`path`, `domain_id`, `type`, `target`, `created`, `lastmodified`, `status`)
       VALUES ('/modify/1234', 14, 'REDIRECT_302', 'https://example.com', NOW(3), NOW(3), 'CREATED');

-- PurlDAOTest

INSERT INTO `user`
  (`id`, `login`, `admin`, `fullname`, `affiliation`, `email`, `password_sha`, `password_reset_token`, `comment`, `created`, `lastmodified`, `status`) 
       VALUES (105,  'user5', false, 'PURL User5', 'My Institution', 'user1@my-institution.org', CONCAT('{SHA-256}', HASH('SHA-256', HASH('SHA-1', 'user4'))), null, 'user5 for PURL server', NOW(3), NOW(3), 'CREATED');

INSERT INTO `domain` (`id`, `path`, `name`, `comment`, `created`, `lastmodified`, `status`) 
       VALUES (15, '/purlDAO', 'Test Domain', '', '2012-07-26 11:20:32.000', '2023-09-21 12:05:06.010', 'MODIFIED');
       
INSERT INTO `domainuser` (`id`, `domain_id`, `user_id`, `can_create`, `can_modify`) 
       VALUES (null, 15, 105, true, false);
       
INSERT INTO `purl` (`path`, `domain_id`, `type`, `target`, `created`, `lastmodified`, `status`)
       VALUES ('/purlDAO/123', 15, 'REDIRECT_302', 'https://example.com', NOW(3), NOW(3), 'CREATED');
