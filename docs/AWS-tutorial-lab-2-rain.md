# AWS Tutorial Lab 2 - Serverless WordPress

See tutorial käib läbi AWS Lab 2 ülesanded, kus loome serverless arhitektuuriga WordPress rakenduse.

---

## Ülevaade

Selles laboris ehitame täisfunktsionaalse WordPress veebilehe, kasutades AWS serverless teenuseid:

- **RDS (Relational Database Service)** - Hallatav MySQL andmebaas
- **ECS (Elastic Container Service) + Fargate** - Konteinerite orkestreerimine ilma serverite haldamiseta
- **ECR (Elastic Container Registry)** - Docker image'ite repositoorium
- **ALB (Application Load Balancer)** - Liikluse jaotamine ja tervise kontrollimine
- **Secrets Manager & Parameter Store** - Turvaline konfiguratsioonihaldus

**Arhitektuur:**
```
Internet → ALB → ECS Fargate (WordPress) → RDS MySQL
                      ↓
                    ECR (Docker Image)
```

**Eeldused:**
- AWS Lab 1 on läbitud
- Docker on arvutis installitud (lokaalne Docker Desktop või WSL2 + Docker)
- AWS CLI installimise juhised on allpool

---

## Sisukord

- [Samm 0 – AWS CLI installimine ja seadistamine (WSL)](#samm-0--aws-cli-installimine-ja-seadistamine-wsl)
- [Samm 1 – IAM Access Keys loomine](#samm-1--iam-access-keys-loomine)
- [Samm 2 – RDS Database Subnet Group loomine](#samm-2--rds-database-subnet-group-loomine)
- [Samm 3 – Database Security Group loomine](#samm-3--database-security-group-loomine)
- [Samm 4 – RDS MySQL Cluster loomine](#samm-4--rds-mysql-cluster-loomine)
- [Samm 5 – Parameter Store parameetrite loomine](#samm-5--parameter-store-parameetrite-loomine)
- [Samm 6 – Secrets Manager verifitseerimine](#samm-6--secrets-manager-verifitseerimine)
- [Samm 7 – ECR Repository loomine ja Docker Image'i üleslaadimine](#samm-7--ecr-repository-loomine-ja-docker-imagei-üleslaadimine)
- [Samm 8 – Application Load Balancer Security Group loomine](#samm-8--application-load-balancer-security-group-loomine)
- [Samm 9 – Target Group loomine](#samm-9--target-group-loomine)
- [Samm 10 – Application Load Balancer loomine](#samm-10--application-load-balancer-loomine)
- [Samm 11 – ECS Task Definition loomine](#samm-11--ecs-task-definition-loomine)
- [Samm 12 – ECS Cluster loomine](#samm-12--ecs-cluster-loomine)
- [Samm 13 – ECS Service loomine](#samm-13--ecs-service-loomine)
- [Samm 14 – WordPress seadistamine ja testimine](#samm-14--wordpress-seadistamine-ja-testimine)
- [Kokkuvõte](#kokkuvõte)

---

## Samm 0 – AWS CLI installimine ja seadistamine (WSL)
[↑ Tagasi sisukorda](#sisukord)

Enne lab'i alustamist peame installima AWS CLI, et saaksime Docker image'eid ECR-i üles laadida.

> **Märkus:** Kui AWS CLI on juba installitud, jäta see samm vahele ja jätka otse Sammuga 1.

### 0.1 WSL terminali avamine

1. Vajuta **Windows + R**
2. Sisesta **wsl** ja vajuta Enter
3. Avaneb Ubuntu/WSL terminal

### 0.2 Süsteemi uuendamine

```bash
sudo apt-get update
```

### 0.3 AWS CLI installimise eeltingimused

Installeeri vajalikud tööriistad:

```bash
sudo apt-get install -y unzip curl
```

### 0.4 AWS CLI v2 allalaadimine

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
```

### 0.5 Zip-faili lahtipakkimine

```bash
unzip awscliv2.zip
```

### 0.6 AWS CLI installimine

```bash
sudo ./aws/install
```

Oodatav väljund:
```
You can now run: /usr/local/bin/aws --version
```

### 0.7 Installatsiooni kontrollimine

```bash
aws --version
```

Oodatav väljund:
```
aws-cli/2.15.x Python/3.11.x Linux/5.x.x-x-generic exe/x86_64.ubuntu.22
```

### 0.8 Puhastamine

Kustuta installimise failid:

```bash
rm -rf aws awscliv2.zip
```

### 0.9 AWS CLI konfigureerimine

AWS CLI konfigureerimist **EI TULE** teha praegu – seda tehakse Sammus 1 pärast IAM access key'de loomist AWS console'is.

---

## Samm 1 – IAM Access Keys loomine
[↑ Tagasi sisukorda](#sisukord)

AWS CLI kasutamiseks vajame IAM access key'sid, mis võimaldavad autentida käsurea kaudu.

### 1.1 Mis on IAM Access Keys?

IAM access keys koosnevad kahest osast:
- **Access Key ID** - avalik identifikaator
- **Secret Access Key** - salajane võti (nagu parool)

Neid kasutatakse AWS CLI, SDK-de ja API päringute autentimiseks.

### 1.2 IAM teenuse avamine

1. AWS Management Console'is, ülaosas otsinguribas otsi **IAM**
2. Vali **IAM** (Identity and Access Management)

### 1.3 Meeskonna kasutaja leidmine

1. Vasakpoolses menüüs vali **Users**
2. Otsi kasutajat **tiim_36**
3. Kliki kasutaja nimel **tiim_36**

### 1.4 Access Key loomine

1. Ava vahekaart **Security credentials**
2. Kerib alla **Access keys** sektsiooni
3. Vajuta **Create access key**
4. Vali **Use case**: **Command Line Interface (CLI)**
5. Märgi linnuke kinnituseks, et mõistad soovitust
6. Vajuta **Next**
7. **(Valikuline)** Lisa kirjeldus: `CLI access for Lab 2`
8. Vajuta **Create access key**

### 1.5 Võtmete salvestamine

**OLULINE:** See on ainus kord, kui saad Secret Access Key näha!

1. Kopeeri **Access key** ja salvesta turvaliselt
2. Kopeeri **Secret access key** ja salvesta turvaliselt
3. Vajuta **Download .csv file** (soovitatud)
4. Vajuta **Done**

> **Hoiatus:** Ära jaga neid võtmeid kellegagi ega lisa neid versioonihaldusesse (git)!

### 1.6 AWS CLI konfigureerimine

Ava terminal (WSL või CMD) ja käivita:

```bash
aws configure
```

Sisesta järgmised väärtused:

```
AWS Access Key ID [None]: <sinu-access-key-id>
AWS Secret Access Key [None]: <sinu-secret-access-key>
Default region name [None]: eu-north-1
Default output format [None]: json
```

**Testimine:**

```bash
aws sts get-caller-identity
```

Peaksid nägema oma kasutaja infot (Account, UserId, Arn).

---

## Samm 2 – RDS Database Subnet Group loomine
[↑ Tagasi sisukorda](#sisukord)

Database subnet group määrab, millistes võrgu alamvõrkudes (subnet) võib andmebaas asuda.

### 2.1 Mis on Database Subnet Group?

Subnet group on kogum alamvõrkudest (subnets) erinevates Availability Zone'ides (AZ), mis tagab andmebaasi kõrge kättesaadavuse ja tõrketaluvuse.

### 2.2 RDS teenuse avamine

1. AWS Management Console'is, ülaosas otsinguribas otsi **RDS**
2. Vali **Aurora and RDS**

### 2.3 Subnet Group loomine

1. Vasakpoolses menüüs vali **Subnet groups**
2. Vajuta **Create DB subnet group**

### 2.4 Subnet Group seadistamine

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Name** | `tiim-36-db-subnet-group-rain` |
| **Description** | `Database subnet group for team 36 - rain` |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |

> **VPC valik:** Ära kasuta VPC-d, mille juures on märge `(DO NOT USE)`! Vali alati `NaVa` VPC.

### 2.5 Availability Zones ja Subnets

1. **Availability Zones** sektsioonis vali **kõik 3 AZ-d**:
   - `eu-north-1a`
   - `eu-north-1b`
   - `eu-north-1c`

2. **Subnets** sektsioonis vali **ainult Database subnets** (mitte Public ega Private subnets):
   - Vali subnets, mille nimedesse kuulub `Database`
   - Peaks olema 3 subnet'i (üks iga AZ kohta)

**Näited õigetest subnet'itest:**
   - Database Subnet AZ A
   - Database Subnet AZ B
   - Database Subnet AZ C

> **Märkus:** Vali kõik 3 Database subnet'i. Ära vali Public ega Private subnet'e!

### 2.6 Subnet Group loomine

Vajuta **Create**

Peaksid nägema kinnitussõnumit: **Successfully created subnet group tiim-36-db-subnet-group-rain**

---

## Samm 3 – Database Security Group loomine
[↑ Tagasi sisukorda](#sisukord)

Andmebaasile on vaja eraldi security group'i, mis lubab ainult sisemist võrguliiklust.

### 3.1 Miks andmebaas vajab eraldi Security Group'i?

Turvalisuse põhimõte: andmebaas ei tohiks olla otse internetist kättesaadav. Security group piirab ligipääsu ainult VPC sisemisele võrgule.

### 3.2 EC2 teenuse avamine

1. Ülaosas otsinguribas otsi **EC2**
2. Vali **EC2**
3. Vasakpoolses menüüs, jaotises **Network & Security**, vali **Security Groups**

### 3.3 Security Group loomine

1. Vajuta **Create security group**
2. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Security group name** | `tiim-36-db-sg-rain` |
| **Description** | `Security group for MySQL database - rain` |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |

### 3.4 Inbound reeglite lisamine

Vajuta **Add rule** ja täida:

| Type | Protocol | Port Range | Source | Description |
|---|---|---|---|---|
| **MYSQL/Aurora** | TCP | 3306 | Custom: `10.0.0.0/16` | MySQL access from VPC |

> **Selgitus:** `10.0.0.0/16` on kogu NaVa VPC sisevõrk. See tähendab, et andmebaasile pääsevad ligi ainult sama VPC sees olevad ressursid.

### 3.5 Tags lisamine

1. Kerib alla **Tags** sektsiooni
2. Lisa järgmised tagid:

| Key | Value |
|---|---|
| `Name` | `tiim-36-db-sg-rain` |
| `Team` | `tiim_36` |

### 3.6 Security Group loomine

Vajuta **Create security group**

---

## Samm 4 – RDS MySQL Cluster loomine
[↑ Tagasi sisukorda](#sisukord)

Nüüd loome hallatava MySQL andmebaasi, mida WordPress kasutab.

### 4.1 Mis on Amazon RDS?

Amazon RDS (Relational Database Service) on hallatav andmebaasi teenus, mis:
- Automatiseerib backup'id ja uuendused
- Pakub kõrge kättesaadavust
- Skaleerub vajadusel
- Haldab patche ja turvalisust

### 4.2 RDS loomise alustamine

1. Mine tagasi **RDS** teenusesse (Aurora and RDS)
2. Vasakpoolses menüüs vali **Databases**
3. Vajuta **Create database**

### 4.3 Database loomise meetod

Vali **Full configuration**

> See annab täieliku kontrolli konfiguratsioooni üle.

### 4.4 Engine valik

1. **Engine type**: Vali **MySQL**
2. **Edition**: Jäta **MySQL Community**
3. **Version**: Jäta vaikimisi versioon (nt MySQL 8.0.35)

### 4.5 Template valik

Vali **Templates**: **Sandbox**

### 4.6 Settings

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **DB instance identifier** | `tiim-36-wordpress-db-rain` |
| **Master username** | Jäta vaikimisi `admin` |
| **Credentials management** | Vali **Managed in AWS Secrets Manager** ✓ |

> **Oluline:** Veendu, et **AWS Secrets Manager** on valitud! See loob automaatselt turvaliselt salvestatud paroolid.

### 4.7 Secrets Manager seadistus

Kui valid "Managed in AWS Secrets Manager":
- Jäta **Encryption key** vaikimisi (aws/secretsmanager)
- AWS loob automaatselt secret'i nimega `rds!db-...`

### 4.8 Instance configuration

1. **DB instance class**: Vali **Burstable classes (includes t classes)**
2. Vali **db.t3.micro** (kõige väiksem ja odavaim)

### 4.9 Storage

Jäta vaikimisi seaded:
- **Storage type**: General Purpose SSD (gp3)
- **Allocated storage**: 20 GiB

### 4.10 Connectivity

Täida järgmised seaded:

| Väli | Väärtus |
|---|---|
| **Compute resource** | Vali **Don't connect to an EC2 compute resource** |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |
| **DB subnet group** | `tiim-36-db-subnet-group-rain` |
| **Public access** | **No** |
| **Existing VPC security groups** | Eemalda **default**, lisa **tiim-36-db-sg-rain** |

### 4.11 Tags (valikuline)

Lisa tag:

| Key | Value |
|---|---|
| **Name** | `tiim-36-wordpress-db-rain` |

### 4.12 Additional configuration

1. Kerib alla ja ava **Additional configuration** sektsioon
2. **Initial database name**: Sisesta `wordpress`

> See loob automaatselt andmebaasi nimega "wordpress", mida WordPress kasutab.

3. **Backup**: Eemalda linnuke (**Enable automated backups** - untick)

### 4.13 Database loomine

1. Kerib alla lõpuni
2. Vaata üle konfiguratsioon
3. Vajuta **Create database**

### 4.14 Ootamine

RDS loomisprotsess võtab **5-10 minutit**.

Olek muutub:
- **Creating** → **Backing-up** → **Available** (roheline)

Samal ajal võid jätkata järgmiste sammudega.

---

## Samm 5 – Parameter Store parameetrite loomine
[↑ Tagasi sisukorda](#sisukord)

Parameter Store salvestab muid konfiguratsiooniväärtusi (mis ei ole nii tundlikud kui paroolid).

### 5.1 Mis on Parameter Store?

AWS Systems Manager Parameter Store võimaldab:
- Salvestada konfiguratsiooniväärtusi (nii tavalisi kui krüpteeritud)
- Tasuta kuni 10,000 parameetrit
- Madalam turvatase kui Secrets Manager (odavam)

**Erinevus Secrets Manager'iga:**
- Secrets Manager: automaatne rotatsioon, kõrgem turvatase, kallis
- Parameter Store: lihtne, odav, pole automaatset rotatsiooni

### 5.2 Parameter Store avamine

1. Ülaosas otsinguribas otsi **Parameter Store**
2. Vali **Parameter Store**

### 5.3 WORDPRESS_DB_NAME parameetri loomine

1. Vajuta **Create parameter**
2. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Name** | `/dev/WORDPRESS_DB_NAME_tiim_36` |
| **Description** | `Database name for WordPress - team 36` |
| **Type** | **SecureString** |
| **KMS Key Source** | **My current account** |
| **KMS Key ID** | Jäta vaikimisi `alias/aws/ssm` |
| **Value** | `wordpress` |

> **Märkus:** Kasutame `SecureString` tüüpi, et väärtus oleks krüpteeritud, kuigi andmebaasi nimi ei ole eriti tundlik info.

3. **Tags — Optional**: Lisa tag

| Key | Value |
|---|---|
| **Name** | `WORDPRESS_DB_NAME_tiim_36` |

4. Vajuta **Create parameter**
5. Peaksid nägema kinnitust: **Create parameter request succeeded!**
6. Vajuta **View details**
7. Kopeeri parameetri **ARN** ja salvesta!

**Näidis:**

| Parameter Name | ARN |
|---|---|
| `WORDPRESS_DB_NAME_tiim_36` | `arn:aws:ssm:eu-north-1:187833180667:parameter/dev/WORDPRESS_DB_NAME_tiim_36` |

### 5.4 WORDPRESS_DB_HOST parameetri loomine

1. Vajuta **Create parameter**
2. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Name** | `/dev/WORDPRESS_DB_HOST_tiim_36` |
| **Description** | `Database endpoint for WordPress - team 36` |
| **Type** | **String** |
| **Data type** | **text** |
| **Value** | `<SIIA-TULEB-RDS-endpoint>:3306` |

**RDS endpoint'i leidmine:**
1. Tee browseris uus tab (duplicate tab)
2. Otsi RDS → Databases → kliki `tiim-36-wordpress-db-rain`
3. Vali **Connect using** alt
4. Kerib alla **Connectivity & security** sektsiooni
5. Kopeeri **Endpoint** (näiteks: `tiim-36-wordpress-db-rain.c3uyeww2anku.eu-north-1.rds.amazonaws.com`)

   > **Märkus:** Kui endpoint'i pole veel näha või on tühi, oota veel mõni minut - RDS andmebaas on veel loomise faasis ja endpoint genereeritakse alles siis, kui andmebaas on täielikult käivitunud ja valmis ühendusi vastu võtma.

6. Mine browseris tagasi Create parameter tabile
7. Otsi üles kast **Value**
8. Lisa endpoint koos pordiga: `<endpoint>:3306` → `tiim-36-wordpress-db-rain.c3uyeww2anku.eu-north-1.rds.amazonaws.com:3306`

3. **Tags — Optional**: Lisa tag

| Key | Value |
|---|---|
| **Name** | `WORDPRESS_DB_HOST_tiim_36` |

4. Vajuta **Create parameter**
5. Peaksid nägema kinnitust: **Create parameter request succeeded!**
6. Vajuta **View details**
7. Kopeeri parameetri **ARN** ja salvesta!

**Näidis:**

| Parameter Name | ARN |
|---|---|
| `WORDPRESS_DB_HOST_tiim_36` | `arn:aws:ssm:eu-north-1:187833180667:parameter/dev/WORDPRESS_DB_HOST_tiim_36` |

---

## Samm 6 – Secrets Manager verifitseerimine
[↑ Tagasi sisukorda](#sisukord)

AWS Secrets Manager salvestab andmebaasi kasutajanime ja parooli turvaliselt.

### 6.1 Mis on AWS Secrets Manager?

Secrets Manager on teenus, mis:
- Salvestab turvaliselt paroolid, API võtmed ja muud salajased väärtused
- Krüpteerib automaatselt
- Võimaldab rotateerida saladusi
- Annab peenokohalise juurdepääsukontrolli

### 6.2 Secrets Manager teenuse avamine

1. Ülaosas otsinguribas otsi **Secrets Manager**
2. Vali **Secrets Manager**

### 6.3 RDS Secret'i leidmine

1. **Secret name** - Peaksid nägema secret'i, mille nimi algab `rds!db-...`
2. Kirjelduses (Description) on näidatud: `The secret associated with the primary RDS DB instance: arn:aws:rds:eu-north-1:187833180667:db:tiim-36-wordpress-db-rain`
3. Kliki selle secret'i nimel

### 6.4 Secret'i detailid

1. Vahekaardil **Secret value** vajuta **Retrieve secret value**
2. Peaksid nägema andmeid ühel kahest viisist:

**Variant 1: Key/value** (tabel formaadis)

| Secret key | Secret value |
|---|---|
| username | admin |
| password | 9x7)!aWl<Vl<DBl<T$8E6\|X813i# |

**Variant 2: Plaintext** (JSON formaadis)

```json
{
  "username": "admin",
  "password": "9x7)!aWl<Vl<DBl<T$8E6|X813i#"
}
```

> **Märkus:** Mõlemad variandid näitavad sama informatsiooni, lihtsalt erinevas formaadis.

### 6.5 ARN kopeerimine

1. Kerib üles **Secret details** sektsiooni
2. Näed järgmist infot:

| Väli | Väärtus |
|---|---|
| **Encryption key** | `aws/secretsmanager` |
| **Secret name** | `rds!db-bca282cb-0735-4624-a499-3bcb96ae9cf4` |
| **Secret ARN** | `arn:aws:secretsmanager:eu-north-1:187833180667:secret:rds!db-bca282cb-0735-4624-a499-3bcb96ae9cf4-Gf1hmh` |
| **Secret description** | `The secret associated with the primary RDS DB instance: arn:aws:rds:eu-north-1:187833180667:db:tiim-36-wordpress-db-rain` |

3. Kopeeri **Secret ARN**
4. Salvesta see – vajad seda hiljem ECS task definition'is!

> **Märkus:** ARN (Amazon Resource Name) on unikaalne identifikaator AWS ressursile.

---

## Samm 7 – ECR Repository loomine ja Docker Image'i üleslaadimine
[↑ Tagasi sisukorda](#sisukord)

ECR (Elastic Container Registry) on AWS-i Docker image'ite repositoorium.

### 7.1 Mis on Amazon ECR?

Amazon ECR on hallatav Docker konteinerite registri teenus:
- Privaatsed repositooriumid
- Integratsioon ECS/EKS-iga
- Automaatne krüpteerimine
- Lihtne juurdepääsuhaldus IAM-iga

### 7.2 ECR teenuse avamine

1. Ülaosas otsinguribas otsi **ECR**
2. Vali **Elastic Container Registry**
3. Vasakpoolses menüüs vali **Repositories**

### 7.3 Repository loomine

1. Vajuta **Create repository**
2. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Visibility settings** | **Private** |
| **Repository name** | `tiim_36/wordpress` |
| **Tag immutability** | Jäta **Mutable** |
| **Encryption** | **AES-256** (vaikimisi) |

3. Vajuta **Create repository**

### 7.4 Docker'i kontrollimine lokaalses masinas

Ava terminal ja kontrolli, et Docker töötab:

```bash
docker --version
```

Peaks näitama midagi sellist:
```
Docker version 24.0.7, build afdd53b
```

Kui Docker ei ole installitud, paigalda:
- **Windows/Mac:** Docker Desktop
- **Linux/WSL:** `sudo apt-get install docker.io`

### 7.5 WordPress image'i tõmbamine

Tõmba ametlik WordPress image Docker Hub'ist:

```bash
docker pull --platform linux/amd64 wordpress:latest
```

> **Märkus:** `--platform linux/amd64` tagab, et image töötab AWS Fargate'is (mis kasutab x86_64 arhitektuuri).

Oodatav väljund:
```
latest: Pulling from library/wordpress
...
Status: Downloaded newer image for wordpress:latest
```

### 7.6 ECR Push Commands

Mine tagasi ECR console'i ja ava äsjaloodud repository `tiim_36/wordpress`.

Vajuta **View push commands** (paremal üleval).

AWS näitab 4 käsku. Järgi neid samme:

#### Samm 1: ECR autentimine

**Käivita esimene käsk** (AWS ECR autentimine):

```bash
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin 187833180667.dkr.ecr.eu-north-1.amazonaws.com
```

Oodatav väljund:
```
Login Succeeded
```

#### Samm 2: Docker image ehitamine (vahele jäetav)

**AWS näitab teist käsku:**

```bash
docker build -t tiim_36/wordpress .
```

> **OLULINE:** Jäta see samm vahele! Me ei ehita oma image'i, vaid kasutame valmis WordPress image'i, mille juba Docker Hub'ist alla laadisime (`docker pull`).

#### Samm 3: Image'i tag'imine

**Käivita kolmas käsk:**

> **OLULINE:** AWS näitab kolmandat käsku nii, **aga see on vale meie puhul:**

**Vale käsk (mida AWS näitab):**
```bash
docker tag tiim_36/wordpress:latest 187833180667.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress:latest
```

> **Meie peame eemaldama algusest `tiim_36/`**, sest meie image nimi on lihtsalt `wordpress:latest` (mille Docker Hub'ist alla laadisime).

**Õige käsk (kasuta seda):**

```bash
docker tag wordpress:latest 187833180667.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress:latest
```

> **Selgitus:** See käsk annab olemasolevale `wordpress:latest` image'ile uue nime, mis viitab sinu ECR repositooriumile.

#### Samm 4: Image'i üleslaadimine ECR-i

**Käivita neljas käsk:**

```bash
docker push 187833180667.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress:latest
```

Oodatav väljund:
```
The push refers to repository [<account-id>.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress]
...
latest: digest: sha256:abc123... size: 12345
```

### 7.7 Verifitseerimine

1. Mine tagasi ECR console'i
2. Kliki repository `tiim_36/wordpress`
3. Peaksid nägema image'it tag'iga `latest`
4. **Kopeeri Image URI** (näiteks: `123456789012.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress:latest`)
5. Salvesta see – vajad seda ECS task definition'is!

---

## Samm 8 – Application Load Balancer Security Group loomine
[↑ Tagasi sisukorda](#sisukord)

ALB vajab eraldi security group'i, mis lubab HTTP liiklust internetist.

### 8.1 Mis on ALB ja miks see vajab Security Group'i?

Application Load Balancer (ALB):
- Jaotab liiklust mitme sihtmärgi vahel (konteinerid, EC2, Lambda)
- Teeb health check'e
- Toetab HTTP/HTTPS marsruutimist

Security group piirab, millised ühendused on lubatud ALB-le.

### 8.2 Security Group loomine

1. Otsi **EC2**
2. Vasakpoolses menüüs, jaotises **Network & Security**, vali **Security Groups**
3. Vajuta **Create security group**
4. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Security group name** | `tiim-36-alb-sg-rain` |
| **Description** | `Security group for Application Load Balancer - rain` |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |

### 8.3 Inbound reeglite lisamine

Vajuta **Add rule** ja täida:

| Type | Protocol | Port Range | Source | Description |
|---|---|---|---|---|
| **HTTP** | TCP | 80 | `0.0.0.0/0` (Anywhere-IPv4) | HTTP access from internet |

> **Selgitus:** ALB peab olema avalikult kättesaadav, seega lubame liikluse kõikidelt IP-aadressidelt.

### 8.4 Tags lisamine

Lisa tagid:

| Key | Value |
|---|---|
| `Name` | `tiim-36-alb-sg-rain` |
| `Team` | `tiim_36` |

### 8.5 Security Group loomine

Vajuta **Create security group**

---

## Samm 9 – Target Group loomine
[↑ Tagasi sisukorda](#sisukord)

Target group määrab, kuhu ALB liikluse edastab.

### 9.1 Mis on Target Group?

Target group on kogum sihtmärke (targets), kuhu load balancer liikluse suunab:
- **IP addresses** - konteinerite IP-d (kasutame ECS Fargate puhul)
- **Instances** - EC2 instantsid
- **Lambda functions** - Serverless funktsioonid

### 9.2 EC2 Load Balancing teenuse avamine

1. Mine **EC2** teenusesse
2. Vasakpoolses menüüs, jaotises **Load Balancing**, vali **Target Groups**

### 9.3 Target Group loomine

1. Vajuta **Create target group**

### 9.4 Target type valik

Vali:
- **Choose a target type**: **IP addresses**

> ECS Fargate kasutab dünaamilisi IP aadresse, seega peame kasutama IP target type'i.

Vajuta **Next**

### 9.5 Target group seadistused

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Target group name** | `tiim-36-tg-rain` |
| **Protocol** | **HTTP** |
| **Port** | **80** |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |
| **Protocol version** | **HTTP1** |

### 9.6 Health check seadistused

Kerib alla **Health checks** sektsiooni:

| Väli | Väärtus |
|---|---|
| **Health check protocol** | **HTTP** |
| **Health check path** | `/wp-admin/images/wordpress-logo.svg` |

> **Selgitus:** See on WordPress staatilise faili aadress, mis alati eksisteerib. ALB kasutab seda, et kontrollida, kas konteiner on terve.

Jäta muud seaded vaikimisi:
- **Healthy threshold**: 5
- **Unhealthy threshold**: 2
- **Timeout**: 5 seconds
- **Interval**: 30 seconds

### 9.7 Tags (valikuline)

**Tags — Optional**: Lisa tag

| Key | Value |
|---|---|
| **Name** | `tiim-36-tg-rain` |

### 9.8 Target group loomine

1. Vajuta **Next**
2. Registreerimata target'eid hetkel ei ole, vajuta uuesti **Next**
3. Vaata üle konfiguratsioon
4. Vajuta **Create target group**

---

## Samm 10 – Application Load Balancer loomine
[↑ Tagasi sisukorda](#sisukord)

Nüüd loome avaliku load balancer'i, mis suunab liikluse konteineritele.

### 10.1 Mis on Application Load Balancer?

ALB on Layer 7 (HTTP/HTTPS) load balancer, mis:
- Töötab mitmes Availability Zone'is (kõrge kättesaadavus)
- Marsruudib liiklust URL põhiselt
- Toetab WebSocket'e ja HTTP/2
- Teeb SSL/TLS terminatsiooni

### 10.2 Load Balancer loomine

1. Mine **EC2** → **Load Balancers**
2. Vajuta **Create load balancer**
3. Vali **Application Load Balancer** → vajuta **Create**

### 10.3 Load balancer seadistused

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Load balancer name** | `tiim-36-alb-rain` |
| **Scheme** | **Internet-facing** |
| **IP address type** | **IPv4** |

### 10.4 Network mapping

1. **VPC**: Vali `vpc-02fb02f6b35b49a55 (NaVa)`
2. **Mappings**: Vali **vähemalt 2 Availability Zone'i**
3. Iga AZ jaoks vali **public subnet**:
   - Näiteks: `Public Subnet AZ A` ja `Public Subnet AZ B`

> **Oluline:** ALB peab olema public subnet'ides, et olla internetist kättesaadav!

**Näidis:**

| Availability Zone | Subnet |
|---|---|
| **eu-north-1a** (eun1-az1) | `subnet-075cc0f55c52f431e` (Public Subnet AZ A) - 10.0.0.0/20 |
| **eu-north-1b** (eun1-az2) | `subnet-0deda9b0ab4690507` (Public Subnet AZ B) - 10.0.16.0/20 |
| **eu-north-1c** (eun1-az3) | `subnet-07d8eb94afbed060d` (Public Subnet AZ C) - 10.0.32.0/20 |

> **Märkus:** Vali vähemalt 2 AZ-d (võid valida kõik 3). Iga subnet vajab vähemalt 8 vaba IP-aadressi, et load balancer saaks efektiivselt skaleeruda.

### 10.5 Security groups

1. Eemalda vaikimisi security group (kliki X)
2. Vali **tiim-36-alb-sg-rain**

### 10.6 Listeners and routing

**Default action** sektsioonis:

| Väli | Väärtus |
|---|---|
| **Protocol** | **HTTP** |
| **Port** | **80** |
| **Routing action** → **Target group** | Vali **tiim-36-tg-rain** |

### 10.7 Listener tags (valikuline)

**Listener tags — Optional**: Võid jätta tühjaks või lisada tag

| Key | Value |
|---|---|
| **Name** | `tiim-36-listener-rain` |

### 10.8 Load balancer loomine

1. Kerib alla lõpuni
2. Vaata üle konfiguratsioon
3. Vajuta **Create load balancer**

### 10.9 Ootamine

Load balancer loomisprotsess võtab **2-3 minutit**. Võid kasutada refresh nuppu (↻ ringjas nool), et staatust uuendada.

**Details** → **Status** olek muutub:
- **Provisioning** → **Active** (roheline)

### 10.10 DNS name kopeerimine

DNS name on nähtav kohe peale load balancer'i loomist:

1. **Details** → **DNS name** - Kopeeri DNS name (näiteks: [tiim-36-alb-rain-1234567890.eu-north-1.elb.amazonaws.com](http://tiim-36-alb-rain-1234567890.eu-north-1.elb.amazonaws.com))
2. Salvesta see – kasutad seda WordPress'i testimiseks!

> **Märkus:** DNS name on nähtav kohe, aga load balancer muutub aktiivseks alles siis, kui Status on **Active**.

---

## Samm 11 – ECS Task Definition loomine
[↑ Tagasi sisukorda](#sisukord)

Task definition on "retsept", mis kirjeldab, kuidas konteiner käivitada.

### 11.1 Mis on Amazon ECS ja Fargate?

**Amazon ECS (Elastic Container Service):**
- Konteinerite orkestreerimise teenus (sarnane Kubernetes'ele, aga lihtsam)
- Käivitab Docker konteinereid

**AWS Fargate:**
- Serverless compute engine ECS-i jaoks
- Pole vaja EC2 instantse hallata
- Maksad ainult kasutatud ressursside eest

### 11.2 Mis on Task Definition?

Task definition määrab:
- Milliseid konteinereid kasutada (image URI)
- Kui palju CPU ja mälu
- Keskkonna muutujad (environment variables)
- Võrguseaded
- IAM rollid

### 11.3 ECS teenuse avamine

1. Ülaosas otsinguribas otsi **ECS**
2. Vali **Elastic Container Service**
3. Vasakpoolses menüüs vali **Task Definitions**

### 11.4 Task definition loomine

1. Vajuta **Create new task definition**

### 11.5 Task definition seadistused

Täida järgmised põhiväljad:

| Väli | Väärtus |
|---|---|
| **Task definition family** | `tiim-36-wordpress-task-rain` |
| **Container name** | `wordpress` |
| **Image URI** | `<sinu-ECR-image-URI>` (kopeeritud sammust 7.7) |

**Kui sa unustasid Image URI salvestada:**
1. Ava uus browser tab
2. Otsi **ECR** → vali **Elastic Container Registry**
3. Kliki repository **tiim_36/wordpress**
4. Kliki **latest** tag'il
5. Kopeeri **Image URI** (näiteks: `187833180667.dkr.ecr.eu-north-1.amazonaws.com/tiim_36/wordpress:latest`)
6. Mine tagasi Task Definition tabile ja kleebi Image URI

### 11.6 Infrastructure requirements

Kerib alla **Infrastructure requirements** sektsiooni:

**Task roles — Conditional:**

| Väli | Väärtus |
|---|---|
| **Task role** | `CustomEcsTaskRole` |
| **Task execution role** | `CustomEcsTaskExecutionRole` |

> **Selgitus:**
> - **Task role** - annab konteinerile õigused kasutada AWS teenuseid (nt S3, DynamoDB)
> - **Task execution role** - annab ECS-ile õigused tõmmata image ECR-ist ja lugeda Secrets Manager/Parameter Store väärtusi

### 11.7 Container seadistused

**Port mappings:**

| Container port | Protocol | Port name | App protocol |
|---|---|---|---|
| **80** | TCP | `wordpress-80-tcp` | HTTP |

### 11.8 Environment variables

Kerib alla **Environment variables** sektsiooni ja lisa järgmised muutujad:

**Kui sa unustasid ARN-e salvestada:**
- **Parameter Store ARN-id:** Otsi **Parameter Store** → kliki parameetri nimel → kopeeri **ARN**
- **Secrets Manager ARN:** Otsi **Secrets Manager** → kliki secret'i nimel → kopeeri **Secret ARN**

**1. WORDPRESS_DB_HOST** (Parameter Store)

| Väli | Väärtus |
|---|---|
| **Key** | `WORDPRESS_DB_HOST` |
| **Value type** | **ValueFrom** |
| **Value** | Kleebi WORDPRESS_DB_HOST parameetri ARN (Samm 5.4) |

> **Näide:** `arn:aws:ssm:eu-north-1:187833180667:parameter/dev/WORDPRESS_DB_HOST_tiim_36`

**2. WORDPRESS_DB_NAME** (Parameter Store)

| Väli | Väärtus |
|---|---|
| **Key** | `WORDPRESS_DB_NAME` |
| **Value type** | **ValueFrom** |
| **Value** | Kleebi WORDPRESS_DB_NAME parameetri ARN (Samm 5.3) |

> **Näide:** `arn:aws:ssm:eu-north-1:187833180667:parameter/dev/WORDPRESS_DB_NAME_tiim_36`

**3. WORDPRESS_DB_USER** (Secrets Manager)

| Väli | Väärtus |
|---|---|
| **Key** | `WORDPRESS_DB_USER` |
| **Value type** | **ValueFrom** |
| **Value** | Kleebi RDS Secret ARN (Samm 6.5) + lisa lõppu `::username::` |

> **Näide:** `arn:aws:secretsmanager:eu-north-1:187833180667:secret:rds!db-bca282cb-0735-4624-a499-3bcb96ae9cf4-Gf1hmh::username::`

**4. WORDPRESS_DB_PASSWORD** (Secrets Manager)

| Väli | Väärtus |
|---|---|
| **Key** | `WORDPRESS_DB_PASSWORD` |
| **Value type** | **ValueFrom** |
| **Value** | Kleebi RDS Secret ARN (Samm 6.5) + lisa lõppu `::password::` |

> **Näide:** `arn:aws:secretsmanager:eu-north-1:187833180667:secret:rds!db-bca282cb-0735-4624-a499-3bcb96ae9cf4-Gf1hmh::password::`

### 11.9 Task definition loomine

1. Kerib alla lõpuni
2. Vaata üle konfiguratsioon
3. Vajuta **Create**

---

## Samm 12 – ECS Cluster loomine
[↑ Tagasi sisukorda](#sisukord)

Cluster on loogiline grupp, kuhu task'id ja service'd kuuluvad.

### 12.1 Mis on ECS Cluster?

ECS cluster on:
- Loogiline konteiner task'idele ja service'tele
- Võib kasutada Fargate (serverless) või EC2 instantse
- Üks cluster võib sisaldada mitmeid service'sid

### 12.2 Cluster loomine

1. Mine **ECS** → **Clusters**
2. Vajuta **Create cluster**

### 12.3 Cluster seadistused

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Cluster name** | `tiim-36-cluster-rain` |
| **Infrastructure** | **AWS Fargate (serverless)** |

Jäta muud seaded vaikimisi.

### 12.4 Cluster loomine

Vajuta **Create**

Cluster luuakse hetkega.

---

## Samm 13 – ECS Service loomine
[↑ Tagasi sisukorda](#sisukord)

Service tagab, et määratud arv task'e on alati käimas.

### 13.1 Mis on ECS Service?

ECS service:
- Haldab task'ide elutsüklit
- Tagab, et soovitud arv task'e on alati käimas
- Integreerub load balancer'iga
- Teeb automaatselt uuendusi ja rollback'e

### 13.2 Service loomine

1. Kliki äsjaloodud cluster'i nimel **tiim-36-cluster-rain**
2. Ava vahekaart **Services**
3. Vajuta **Create**

### 13.3 Environment

| Väli | Väärtus |
|---|---|
| **Compute options** | **Launch type** |
| **Launch type** | **FARGATE** |

### 13.4 Deployment configuration

| Väli | Väärtus |
|---|---|
| **Application type** | **Service** |
| **Task definition** | Vali **tiim-36-wordpress-task-rain** (viimane versioon) |
| **Service name** | `tiim-36-service-rain` |
| **Desired tasks** | **1** |

### 13.5 Networking

Ava **Networking** sektsioon:

| Väli | Väärtus |
|---|---|
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |
| **Subnets** | Vali **Private subnets** (mitte Public!) |
| **Security group** | Vali **Use an existing security group** → `tiim-36-alb-sg-rain` |
| **Public IP** | **TURNED OFF** (disabled) |

> **Oluline:** Konteinerid peavad olema private subnet'ides, kuna need saavad liikluse ALB kaudu, mitte otse internetist.

### 13.6 Load balancing

Ava **Load balancing** sektsioon:

| Väli | Väärtus |
|---|---|
| **Load balancer type** | **Application Load Balancer** |
| **Load balancer** | Vali **Use an existing load balancer** → `tiim-36-alb-rain` |
| **Listener** | **Use an existing listener** → **80:HTTP** |
| **Target group** | Vali **Use an existing target group** → `tiim-36-tg-rain` |

### 13.7 Service loomine

1. Kerib alla lõpuni
2. Vaata üle konfiguratsioon
3. Vajuta **Create**

### 13.8 Service käivitamise jälgimine

1. Kliki loodud service'il **tiim-36-service-rain**
2. Ava vahekaart **Tasks**
3. Peaksid nägema task'i, mille olek:
   - **Provisioning** → **Pending** → **Running** (roheline)

See võib võtta **2-5 minutit**.

### 13.9 Health check verifitseerimine

1. Mine **EC2** → **Target Groups** → kliki `tiim-36-tg-rain`
2. Ava vahekaart **Targets**
3. Peaksid nägema target'i (IP aadressi), mille olek:
   - **Initial** → **Unhealthy** → **Healthy** (roheline)

Kui olek on **Healthy**, WordPress on valmis!

---

## Samm 14 – WordPress seadistamine ja testimine
[↑ Tagasi sisukorda](#sisukord)

Nüüd pääseme lõpuks WordPress'i kätte ja seadistame selle.

### 14.1 WordPress'i avamine

1. Ava brauser
2. Sisesta ALB DNS name (kopeeritud sammust 10.9)
   - Näiteks: `http://tiim-36-alb-rain-1234567890.eu-north-1.elb.amazonaws.com`
3. Vajuta Enter

Peaksid nägema WordPress'i installatsiooni lehte!

> Kui leht ei laadi, oota veel 1-2 minutit – konteiner võib veel käivituda.

### 14.2 WordPress keele valik

1. Vali keel (näiteks **English (United States)** või **Eesti**)
2. Vajuta **Continue**

### 14.3 WordPress seadistamine

Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Site Title** | `Team 36 WordPress` |
| **Username** | `admin` (või oma nimi) |
| **Password** | (tugev parool, salvesta see!) |
| **Your Email** | sinu.email@example.com |
| **Search engine visibility** | Võid linnukese panna (lab'i jaoks) |

Vajuta **Install WordPress**

### 14.4 Sisselogimine

1. Kui näed "Success!" sõnumit, vajuta **Log In**
2. Sisesta kasutajanimi ja parool
3. Vajuta **Log In**

Peaksid nägema WordPress'i admin dashboardi!

### 14.5 Andmebaasi ühenduse verifitseerimine

WordPress on edukalt ühendatud RDS MySQL andmebaasiga, kui:
- Installatsiooniprotsess õnnestus
- Saad sisse logida
- Näed admin dashboardi

### 14.6 Esimese postituse loomine

1. Vasakpoolses menüüs vali **Posts** → **Add New**
2. Sisesta pealkiri: "Hello from AWS Fargate!"
3. Kirjuta sisu: "This WordPress site runs on serverless infrastructure: ECS Fargate + RDS MySQL + ALB"
4. Vajuta **Publish** kaks korda

### 14.7 Lehe vaatamine

1. Paremal üleval vajuta **Visit Site**
2. Peaksid nägema oma WordPress'i avalehte koos postitusega!

**Palju õnne!** WordPress töötab AWS serverless arhitektuuril.

---

## Kokkuvõte
[↑ Tagasi sisukorda](#sisukord)

Suurepärane töö! Oled edukalt läbinud AWS Lab 2 ja loonud täisfunktsionaalse serverless WordPress rakenduse.

**Õpitud oskused:**

- ✅ IAM Access Keys loomine CLI kasutamiseks
- ✅ RDS Database Subnet Group ja MySQL andmebaasi konfigureerimine
- ✅ AWS Secrets Manager ja Parameter Store kasutamine turvaliseks konfiguratsioonihalduseks
- ✅ Docker image'ite haldamine Amazon ECR-is
- ✅ Application Load Balancer seadistamine koos target group'idega
- ✅ ECS Task Definition loomine keskkonna muutujatega
- ✅ ECS Fargate cluster'i ja service'i haldamine
- ✅ Serverless arhitektuuri lõpuni ehitamine

**Arhitektuur ülevaade:**

```
Internet
   ↓
Application Load Balancer (Public Subnets)
   ↓
ECS Fargate Service (Private Subnets)
   ↓
WordPress Container (ECR)
   ↓
RDS MySQL Database (Database Subnets)
   ↓
Secrets Manager + Parameter Store
```

**Ressursside kokkuvõte:**

| Ressurss | Nimi |
|---|---|
| RDS Database | `tiim-36-wordpress-db-rain` |
| ECR Repository | `tiim_36/wordpress` |
| Target Group | `tiim-36-tg-rain` |
| Load Balancer | `tiim-36-alb-rain` |
| ECS Cluster | `tiim-36-cluster-rain` |
| ECS Service | `tiim-36-service-rain` |
| Task Definition | `tiim-36-wordpress-task-rain` |

**Cleanup (valikuline):**

Kui soovid ressursse kustutada (kursuse lõpus):
1. Kustuta ECS Service
2. Kustuta ECS Cluster
3. Kustuta Load Balancer
4. Kustuta Target Group
5. Kustuta RDS Database (võtab ~5 min)
6. Kustuta Security Groups
7. Kustuta ECR Repository
8. Kustuta Parameter Store parameetrid
9. Kustuta Secrets Manager secret

> **Hoiatus:** Ära kustuta ressursse, kui teine tiimiliige veel kasutab!

---

**Lab 2 on lõppenud!**
