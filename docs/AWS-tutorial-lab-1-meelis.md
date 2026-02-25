# AWS Tutorial Lab 1

See tutorial käib läbi AWS Lab 1 ülesanded.

---

## Samm 1 – AWS Console'i sisselogimine

### 1.1 Sisselogimine meeskonna kontoga

Meie meeskonna jagatud AWS konto:

| Väli | Väärtus |
|---|---|
| **Console URL** | https://187833180667.signin.aws.amazon.com/console |
| **Kasutajanimi** | `tiim_36` |
| **Parool** | `@Vk,Me0GvV*d2ola6@` |

**Sisselogimise sammud:**

1. Ava brauser ja mine aadressile: https://187833180667.signin.aws.amazon.com/console
2. Sisesta **IAM user name**: `tiim_36`
3. Sisesta **Password**: `@Vk,Me0GvV*d2ola6@`
4. Vajuta **Sign in**

Pärast edukat sisselogimist peaksid nägema AWS Management Console'i dashboardi.

> **Märkus:** See on meeskonna jagatud konto – kõik tiimiliikmed kasutavad samu kredentsiaale. Ole ettevaatlik ressursside loomisel ja kustutamisel, et mitte teiste tööd häirida.

---

## Samm 2 – Security Group loomine

Security Group on virtuaalne firewall, mis kontrollib sissetulevat (inbound) ja väljuvat (outbound) võrguliiklust EC2 instantsidele.

### 2.1 EC2 teenuse avamine

1. AWS Management Console'is, ülaosas otsinguribas otsi **EC2**
2. Vali **EC2** (Virtual Servers in the Cloud)

### 2.2 Security Group loomine

1. Vasakpoolses menüüs, jaotises **Network & Security**, vali **Security Groups**
2. Vajuta **Create security group**
3. Täida järgmised väljad:

| Väli | Väärtus |
|---|---|
| **Security group name** | `tiim-36-web-sg-meelis` |
| **Description** | `Security group for web server - meelis` |
| **VPC** | `vpc-02fb02f6b35b49a55 (NaVa)` |

> **Nimetamiskonventsioon:** Kasutame formaati `tiim-36-<ressurss>-<sinu-nimi>`, et eristada enda ressursse teiste tiimiliikmete omadest jagatud kontol. Näiteks: `tiim-36-web-sg-meelis`

> **VPC valik:** Ära kasuta VPC-d, mille juures on märge `(DO NOT USE)`! Vali alati VPC, mille juures on märge `(NaVa)` – see on õige kursusel kasutatav VPC.

### 2.3 Inbound reeglite lisamine

Inbound reeglid määravad, milline liiklus võib serverisse siseneda.

**Lisa järgmised reeglid** (vajuta **Add rule** iga reegli jaoks):

| Type | Protocol | Port Range | Source | Description |
|---|---|---|---|---|
| SSH | TCP | 22 | 0.0.0.0/0 (Anywhere-IPv4) | SSH access |
| HTTP | TCP | 80 | 0.0.0.0/0 (Anywhere-IPv4) | HTTP web traffic |

> **Märkus:** `0.0.0.0/0` tähendab, et ühendused on lubatud kõikidelt IP-aadressidelt. Tootmiskeskkonnas tuleks seda piirata.

### 2.4 Outbound reeglid

Vaikimisi on outbound reeglid juba seadistatud (All traffic → 0.0.0.0/0). Jäta need samaks.

### 2.5 Tags - optional

Tagid aitavad ressursse organiseerida ja identifitseerida.

**Lisa tag** (valikuline, aga soovituslik):

1. Kerib alla **Tags** sektsiooni
2. Vajuta **Add new tag**
3. Täida:

| Key | Value |
|---|---|
| `Name` | `tiim-36-web-sg-meelis` |

> Tagid aitavad jagatud kontol ressursse eristada ja hallata.

### 2.6 Security Group loomine

Vajuta **Create security group**

Pärast loomist peaksid nägema kinnitussõnumit ja Security Group ID-d. Näiteks: `sg-0b0819740fbc2e6bc - tiim-36-web-sg-meelis`. Kirjuta see ID üles – vajad seda EC2 instants'i loomisel.

---

## Samm 3 – EC2 Instance'i loomine

EC2 (Elastic Compute Cloud) on AWS-i virtuaalne server, kus saame veebilehte hostida.

### 3.1 Launch Instances

1. Vasakpoolses menüüs, jaotises **Instances**, vali **Instances**
2. Vajuta **Launch instances**

### 3.2 Name and tags

Sisesta instance'i nimi:

| Väli | Väärtus |
|---|---|
| **Name** | `tiim-36-web-server-meelis` |

> Tag aitab instance'i hiljem kergesti identifitseerida.

Kliki **Add additional tags**.

### 3.3 Key pair (login)

Key pair võimaldab turvalist SSH ligipääsu serverisse. Selle lab'i jaoks ei ole võtmepaari vaja.

Vali rippmenüüst **Proceed without a key pair (Not recommended)**

> **Märkus:** Tootmiskeskkonnas tuleks alati kasutada SSH key pair'i turvalisuse tagamiseks.

### 3.4 Network settings

1. Vajuta **Edit** (Network settings sektsiooni paremal pool)
2. Seadista järgmised väärtused:

| Väli | Väärtus |
|---|---|
| **VPC - required** | `vpc-02fb02f6b35b49a55 (NaVa)` |
| **Subnet** | `Public Subnet AZ A` |

> **Oluline:** Ära vali VPC-d, mille juures on märge `(DO NOT USE)`! Subnet peab jääma Public Subnet AZ A.

**Firewall (security groups):**

3. Vali **Select existing security group**
4. Märgi linnuke security group'i `tiim-36-web-sg-meelis` (või sinu loodud security group) ees

> See lubab HTTP liiklust (port 80) instance'ile.

### 3.5 Advanced details

Kerib alla **Advanced details** sektsiooni ja leia **User data - optional** väli.

**User data** skript käivitatakse automaatselt, kui instance esimest korda käivitub. Kasutame seda veebiserveri installimiseks.

**Variant 1 – ClaudeCode parandatud versioon (Amazon Linux 2023):**

```bash
#!/bin/bash
yum update -y
yum install httpd -y
systemctl start httpd
systemctl enable httpd
cd /var/www/html
echo "<html><h1>meelis</h1></html>" > index.html
```

**Variant 2 – Õpetaja originaalversioon (vanemad Amazon Linux versioonid):**

```bash
#!/bin/bash
yum update -y
yum install httpd -y
service httpd start
chkconfig httpd on
cd /var/www/html
echo "<html><h1>meelis</h1></html>" > index.html
```

> **Selgitus:**
> - **Variant 1** kasutab `systemctl` käske – töötab Amazon Linux 2023 ja uuemate versioonidega
> - **Variant 2** kasutab `service` ja `chkconfig` käske – töötab Amazon Linux 2 ja vanemate versioonidega
> - Mõlemad installivad Apache HTTP serveri (`httpd`), käivitavad selle ja loovad lihtsa HTML lehe tekstiga "meelis"

> **Troubleshooting:**
> - Kui kumbki skript ei töötanud, kontrolli **Application and OS Images (Amazon Machine Image)** sektsiooni sammus 3.1
> - Veendu, et valid **Amazon Linux 2023 AMI** (vaikimisi valik) – kasuta siis Variant 1
> - Kui valisid muu AMI (nt Ubuntu), kasuta hoopis `apt-get install apache2` käsku

### 3.6 Launch instance

Vajuta paremal pool üleval oranžit nuppu **Launch instance**.

AWS hakkab nüüd looma EC2 instance'i. See võib võtta mõne minuti.

Pärast edukalt loomist näed kinnitussõnumit ja instance ID-d. Näiteks: `i-0123456789abcdef0`

---

## Samm 4 – Instance'i verifitseerimine

### 4.1 Instance'i leidmine

1. Vasakpoolses menüüs vali **Instances**
2. Otsinguribas **Find Instance by attribute or tag (case-sensitive)** sisesta: `tiim-36-web-server-meelis`
3. Peaks ilmuma sinu loodud instance

### 4.2 Instance State kontrollimine

Kontrolli, et instance'i olek (**Instance State**) on **Running** (roheline).

Kui olek on **Pending** (kollane), oota mõni minut ja värskenda lehte – instance käivitub.

### 4.3 Veebilehe testimine

1. Kliki instance'i nimel (`tiim-36-web-server-meelis`), et avada **Instance summary**
2. Sektsioonis **Info** leia **Public IPv4 address** (nt `54.123.45.67`)
3. Kliki IP aadressi kõrval olevat **open address** linki (või kopeeri IP aadress ja ava brauseris)

Peaksid nägema lehte tekstiga: **meelis**

> Kui lehte ei laadita kohe, oota 1-2 minutit – User data skript võib veel käivituda. Seejärel proovi uuesti.

### 4.4 Troubleshooting

> **Märkus:** Rain ei saanud nime kuvamist brauseris tööle. Hilisemas sammus (5.3) serverist otse kontrollides selgus aga, et User Data skript lõi õigesti HTML faili.

---

## Samm 5 – Serveriga ühendamine

AWS võimaldab ühenduda EC2 instance'iga otse brauseris EC2 Instance Connect funktsiooniga.

### 5.1 Instance Connect avamine

1. Vasakpoolses menüüs vali **Instances**
2. Leia oma instance **tiim-36-web-server-meelis** (või otsi otsinguribas)
3. **Märgi linnuke** instance'i nime ees
4. Ülevalt paremalt vajuta **Connect**
5. EC2 Instance Connect vahekaardil (peaks olema vaikimisi valitud), alt paremalt vajuta **Connect**

### 5.2 Ühenduse tulemus

Pärast edukast ühendust avaneb uus brauser vahekaart terminali akendega. Peaksid nägema Amazon Linux 2023 tervitusteksti:

```
   ,     #_
   ~\_  ####_        Amazon Linux 2023
  ~~  \_#####\
  ~~     \###|
  ~~       \#/ ___   https://aws.amazon.com/linux/amazon-linux-2023
   ~~       V~' '->
    ~~~         /
      ~~._.   _/
         _/ _/
       _/m/'
[ec2-user@ip-10-0-14-0 ~]$
```

Oled nüüd ühendatud oma EC2 instance'iga ja saad käsurealt käske sisestada.

> **Märkus:** `ec2-user` on vaikimisi kasutajanimi Amazon Linux süsteemides. IP aadress (`ip-10-0-14-0`) on instance'i privaatne IP aadress VPC sisevõrgus.

### 5.3 Veebilehe sisu kontrollimine

Saad kontrollida, kas User Data skript lõi õigesti HTML faili.

**Käsk:**

```bash
cat /var/www/html/index.html
```

**Oodatav tulemus:**

```
<html><h1>meelis</h1></html>
```

Kui näed ülalpool olevat HTML koodi, siis veebileht on õigesti loodud ja peaks brauseris kuvama teksti "meelis".

---

## Samm 6 – IAM Role loomine S3 ligipääsuks

AWS S3 (Simple Storage Service) on objektipõhine salvestusteenus. Et EC2 instance saaks S3-ga suhelda, peame looma IAM rolli, mis annab vajalikud õigused.

### 6.1 S3 ligipääsu testimine

Proovime serveris käivitada AWS S3 käsku:

**Käsk:**

```bash
aws s3 ls
```

**Oodatav tulemus (viga):**

```
Unable to locate credentials. You can configure credentials by running "aws login".
```

See viga on oodatav – instance'il ei ole veel S3 ligipääsuks vajalikke õigusi.

### 6.2 IAM teenuse avamine

1. Mine tagasi **AWS Management Console'i** (võid jätta EC2 Instance Connect'i akna avatuks)
2. Ülaosas otsinguribas otsi **IAM**
3. Vali **IAM** (Identity and Access Management)

### 6.3 IAM Role loomine

1. Vasakpoolses menüüs, jaotises **Access Management**, vali **Roles**
2. Paremal ülevalt vajuta **Create role**

### 6.4 Trusted entity type seadistamine

**Trusted entity type:**
- Jäta valituks **AWS service** (vaikimisi)

**Use case:**
1. Jaotises **Service or use case** vali rippmenüüst **EC2**
2. Jäta all olev valik **EC2** (peaks olema automaatselt valitud)
3. Vajuta **Next**

### 6.5 Permissions policy lisamine

1. Otsingukasti **Permissions policies** all sisesta **s3**
2. Märgi linnuke rolli **AmazonS3FullAccess** ees
3. Vajuta **Next**

> **Märkus:** `AmazonS3FullAccess` annab täielikud õigused kõikidele S3 toimingutele. Tootmiskeskkonnas tuleks kasutada kitsendatud õigusi (principle of least privilege).

### 6.6 Role detailid

Sisesta rolli nimi:

| Väli | Väärtus |
|---|---|
| **Role name** | `tiim-36-web-s3-access-meelis` |

**Tags (valikuline, aga soovituslik):**

1. Kerib alla **Tags** sektsiooni
2. Vajuta **Add new tag**
3. Täida:

| Key | Value |
|---|---|
| `Name` | `tiim-36-web-s3-access-meelis` |

### 6.7 Role loomine

Vajuta **Create role**

Pärast loomist peaksid nägema kinnitussõnumit: **Role tiim-36-web-s3-access-meelis created**

---

## Samm 7 – IAM Role'i lisamine EC2 Instance'ile

Nüüd peame loodud IAM rolli siduma oma EC2 instance'iga, et instance saaks S3-le ligi pääseda.

### 7.1 EC2 Instances lehele navigeerimine

1. Ülaosas otsinguribas otsi **EC2**
2. Vali **EC2**
3. Vasakpoolses menüüs vali **Instances**

### 7.2 Instance'i leidmine ja IAM rolli muutmine

1. Otsi ja leia instance **tiim-36-web-server-meelis**
2. **Märgi linnuke** instance'i nime ees
3. Paremal ülevalt vajuta **Actions**
4. Vali **Security** → **Modify IAM role**

### 7.3 IAM rolli valimine

1. Otsinguribas otsi **tiim-36-web-s3-access-meelis**
2. Vali rippmenüüst **tiim-36-web-s3-access-meelis**
3. Vajuta **Update IAM role**

Peaksid nägema kinnitussõnumit, et IAM roll on edukalt lisatud instance'ile.

### 7.4 S3 ligipääsu verifitseerimine

Mine tagasi **EC2 Instance Connect** aknasse (kus serveriga ühendus on) ja käivita uuesti S3 käsk:

**Käsk:**

```bash
aws s3 ls
```

**Oodatav tulemus:**

```
[ec2-user@ip-10-0-14-0 ~]$ aws s3 ls
2026-01-25 17:49:41 8cdcdde0a630445798b44da3a4dfc33d-logs
2026-02-02 19:37:42 nava-terraform-state
2026-02-08 15:27:41 nava-terraform-state-for-students
2026-02-15 23:15:36 team-26-terraform-state
```

Kui näed S3 bucket'ite nimekirja (sarnane ülalpool olevale), siis IAM roll töötab õigesti ja instance'il on nüüd ligipääs S3 teenusele!

> **Märkus:** S3 bucket'ite nimekiri võib sinu puhul erineda – see on kursusel kasutatavate bucket'ite nimekiri AWS kontol.

---

## Kokkuvõte

Palju õnne! Oled edukalt läbinud AWS Lab 1 ja õppinud järgmisi oskusi:

- ✅ AWS Console'i sisselogimine IAM kasutajaga
- ✅ Security Group'i loomine ja konfigureerimine (HTTP, SSH)
- ✅ EC2 Instance'i käivitamine User Data skriptiga
- ✅ EC2 Instance'iga ühendamine EC2 Instance Connect kaudu
- ✅ IAM Role'i loomine S3 ligipääsuks
- ✅ IAM Role'i lisamine EC2 Instance'ile
- ✅ AWS CLI kasutamine S3 teenusega suhtlemiseks

Need on AWS pilve põhioskused, mida kasutad edaspidi keerukamate lahenduste loomiseks.

---

**Lab 1 on lõppenud!**
