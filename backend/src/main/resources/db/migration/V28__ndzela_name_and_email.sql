-- V28__ndzela_name_and_email.sql: Gabon's company is written Ndzela, with its new e-mail and WhatsApp number.
UPDATE countries SET legal_name = 'Ndzela', legal_email = 'ndzelagabon@gmail.com', support_email = 'ndzelagabon@gmail.com', support_whatsapp = '+241 02 82 18 55'
WHERE code = 'GA';
