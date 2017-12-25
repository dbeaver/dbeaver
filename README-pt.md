
[![Build Status](https://travis-ci.org/dbeaver/dbeaver.svg?branch=devel)](https://travis-ci.org/dbeaver/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/93fcfdba7805406298b2e60c9d56f50e)](https://www.codacy.com/app/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dbeaver/dbeaver&amp;utm_campaign=Badge_Grade)

<img src="https://github.com/dbeaver/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>
# DBeaver

Ferramenta de acesso a bancos de dados multiplataforma para desenvolvedores, programadores SQL, DBAs e analistas.
Suporta qualquer SGBD que tenha um driver JDBC (o que basicamente quer dizer - QUALQUER SGBD). Também pode funcionar com SGBDs que não possuem driver JDBC (WMI, MongoDB, Cassandra, Redis).

* Possui inúmeros <a href="https://dbeaver.jkiss.org/docs/features/">recursos</a>.
* Baseado na plataforma <a href="http://www.eclipse.org/">Eclipse</a>.
* Utiliza plataforma de plugins e provê funcionalidades adicionais para os SGBDs mais populares (MySQL, PostgreSQL, Oracle, DB2 na versão 3.7.x).

<a href="https://dbeaver.jkiss.org/product/dbeaver-ss-classic.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-classic.png" width="400"/></a>
<a href="https://dbeaver.jkiss.org/product/dbeaver-ss-dark.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-dark.png" width="400"/></a>

## Download

Você pode baixar os arquivos pré compilados em https://dbeaver.jkiss.org/download/

## Compilação

#### Pré-requisitos:
 1. Java (JDK) 1.8+
 2. Apache Maven 3+
 3. Acesso à Internet

#### Compilar
```sh
git clone https://github.com/dbeaver/dbeaver.git dbeaver
cd dbeaver
mvn install
```
Arquivos em `product/standalone/target/products`

## Observações

- Por favor, deixe os relatórios de bugs e pedidos de novas funcionalidades no <a href="https://github.com/dbeaver/dbeaver/issues">GitHub issue tracker</a>.
- DBeaver é um projeto sem intenções lucrativas. Por favor, não espere ações imediatas aos relatos.
- Caso tenha dúvidas, sugestões, ideias, etc - <a href="mailto:serge@jkiss.org">fale comigo</a>.
- Pull requests são bem vindos.
- Visite https://dbeaver.jkiss.org para mais informação.
- Obrigado por utilizar DBeaver! Marque com estrela se gostar.
