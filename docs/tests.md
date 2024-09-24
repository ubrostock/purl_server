PURL-Server Tests
-----------------
### Aufgabe(n)
- Sicherstellung der Funktionalität der Software durch umfangreiche Tests
- Aktualisierung der Bestehenden Tests (RestAPI) gegen eine automatisch eingerichtete Umgebung
  (für den derzeitigen Test muss eine Datenbank separat installiert sein, die mit bestimmten


### Literatur / Quellen
- Atlassian: "Die unterschiedlichen Arten von Tests"
  https://www.atlassian.com/de/continuous-delivery/software-testing/types-of-software-testing
- Baldung: "A Guide to JUnit 5"
  https://www.baeldung.com/junit-5
- Baldung: "Testing in Spring Boot"
  https://www.baeldung.com/spring-boot-testing
- Medium: "Setting Up H2 Database for Integration Testing in a Spring Boot Application"
  https://medium.com/@__aditya45__/setting-up-h2-database-for-integration-testing-in-a-spring-boot-application-f3612a4e01ed  
- Medium: "How to Test JavaMailSender... Using GreenMail"
  https://medium.com/@erica35225/how-to-test-javamailsender-send-functionality-using-greenmail-f4384a38ee91
  
### Kurzprotokoll
- Update Spring-Boot-Test-Dependency
- Junit4 entfernt -> Bestehende Klasse an Junit5 angepasst (Reihenfolge der Parameter in assert*-Methoden, Message am Ende)  
- application.properties für Tests in /src/test/resources
  - JPA-Konfiguration entfernt (wir nutzen nur PlainSQL)
  - datasource.schema + datasource.data Properties aktualisiert
- H2 hat Probleme mit Aliasen für Tabellen in JOIN-Statements 
  (Beispiel: select o.*, l.*  from owner o join link l on o.link=l.id)
  Diese lassen  sich nicht via resultset.getXXX(name) verarbeiten.
  Wir können nur den ganzen TabellenNamen als Alias nutzen.
  https://github.com/h2database/h2database/issues/2581
  https://github.com/ubrostock/purl_server/commit/b02c3954de5a988e307cc31232f5a052a45e268d

- Um eine REST-Request mit Anmeldung zu testen brauchen wir   
  die Annotation @WithMockUser("user1") und müssen den Authentication Header setzen.
