# Данный проект имитирует систему для оформления и оплаты заказов в одном интернет магазине
![image](https://user-images.githubusercontent.com/64843863/179354555-d228797c-88fe-4ed5-bec7-36c069c2d11c.png)
### C1, C2 - это клиенты, которые взаимодействуют с сервисами по http; https://github.com/Sirenitix/spring-integration-order-payment
### RabbitMQ - брокер сообщений, который занимается созданием и управлением queue и exchange.
### Core - это сервис, который имеет доступ к базе данных. Реализует CRUD для заказов.
### Mailing - сервис для отправки писем. https://github.com/Sirenitix/spring-integration-mailing
