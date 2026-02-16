# Tutorial: Deployment Workshop (Ülesanne 4)

See tutorial käib läbi kõik viis sammu ülesandest `Task-4.md`, kasutades meie **Borsibaar** projekti kui näidet.

---

## Eeldused

- Sul on SSH ligipääs serverile
- Kohalikul masinal on Git, Docker ja Maven installeeritud
- Projekti kood on kloonitud

---

## Samm 1 – Serveri seadistamine

### 1.1 SSH ühenduse loomine

SSH (Secure Shell) on krüpteeritud protokoll, mis võimaldab serverit kaugjuhtida.

Meie tiimi server:

```bash
ssh ubuntu@193.40.157.110
```

Kontrolli, et oled õiges serveris:

```bash
whoami
hostname
ls /opt/
```

Loo oma testi kaust, et näidata enda ligipääsu:

```bash
sudo  mkdir /opt/sinuNimi/
echo "test" > /opt/sinuNimi/test.txt
```

### 1.2 Dockeri installimine

Docker võimaldab rakendust isoleeritud konteineris käivitada – sama image töötab ühtmoodi igal masinal.

```bash
# Uuenda paketinimekiri
sudo apt-get update

# Installi Docker
sudo apt-get install -y docker.io
sudo apt-get install -y docker-compose

# Lisa oma kasutaja docker gruppi (et ei peaks sudo kasutama)
sudo usermod -aG docker $USER

# Logi välja ja uuesti sisse, seejärel kontrolli:
docker --version
docker-compose --version
```

### 1.3 NGINX installimine (kui kasutatakse ilma Dockerita)

Meie projektis jookseb NGINX Dockeri konteineris (`docker-compose.prod.yaml`), seega eraldi installimist ei ole vaja. Aga teadmiseks:

```bash
sudo apt-get install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

---

## Samm 2 – Manuaalne Deploy

### 2.1 Docker image'ite loomine lokaalsel masinal

Meil on kaks Dockerfilet:
- `backend/Dockerfile` – Spring Boot rakendus
- `frontend/Dockerfile` – Next.js rakendus

**Eeldus: Java 21 peab olema WSL-is installitud.**
Kui saad vea `JAVA_HOME is not defined correctly`, installi Java:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk
java -version
```

```bash
# Projekti juurkaustas

# Enne image'i ehitamist tuleb backend kompileerida – Dockerfile kopeerib JAR faili
# Kui saad vea "AccessDeniedException: /home/.../.m2/repository", käivita:
#   sudo chown -R $USER:$USER ~/.m2
cd backend && ./mvnw clean package -DskipTests && cd ..

# Ehita backend image
docker build -t borsibaar-backend:latest ./backend

# Ehita frontend image (build-arg on vajalik Next.js client-side URL jaoks)
docker build \
  --build-arg NEXT_PUBLIC_BACKEND_URL=http://SERVER_IP \
  -t borsibaar-frontend:latest \
  -f frontend/Dockerfile .
```

### 2.2 Image'ite ülesladimine registrisse

Kasuta Docker Hub-i (tasuta) või GitHub Container Registry-t.

**Docker Hub:**

```bash
# Logi sisse
docker login

# Taga image (asenda 'janx4u' oma Docker Hub kasutajanimega)
docker tag borsibaar-backend:latest janx4u/borsibaar-backend:latest
docker tag borsibaar-frontend:latest janx4u/borsibaar-frontend:latest

# Lae üles
docker push janx4u/borsibaar-backend:latest
docker push janx4u/borsibaar-frontend:latest
```

**GitHub Container Registry:**

```bash
# Logi sisse GitHub PAT tokeniga
echo $GITHUB_TOKEN | docker login ghcr.io -u sinuGithubNimi --password-stdin

docker tag borsibaar-backend:latest ghcr.io/sinuOrg/borsibaar-backend:latest
docker push ghcr.io/sinuOrg/borsibaar-backend:latest
```

### 2.3 Serveris rakenduse käivitamine

SSH serverisse ja loo kaust:

```bash
sudo mkdir -p /opt/tiim-36/borsibaar
cd /opt/tiim-36/borsibaar
```

Loo `.env` fail keskkonna muutujatega (ära kommiteeri paroole Giti!):

```bash
cat > .env << 'EOF'
POSTGRES_USER=borsibaar_user
POSTGRES_PASSWORD=dev_password_123
POSTGRES_DB=borsibaar_dev
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/borsibaar_dev
SPRING_DATASOURCE_USERNAME=borsibaar_user
SPRING_DATASOURCE_PASSWORD=dev_password_123
GOOGLE_CLIENT_ID=449588174358-25n6dd230ms5fd7roe42dn147p5p3ap9.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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
JWT_SECRET="Ot6RFlPxRM8brShoZFUAHrVFOmMZLTjayJelu0+Etqg="
APP_CORS_ALLOWED_ORIGINS=http://193.40.157.110
APP_FRONTEND_URL=http://193.40.157.110
NEXT_PUBLIC_BACKEND_URL=http://193.40.157.110
EOF
```

Loo `docker-compose.yml` (kohandatud sinu image nimedega):

```yaml
version: "3.3"
services:
  postgres:
    image: postgres:17
    container_name: borsibaar-db
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - borsibaar-network

  backend:
    image: janx4u/borsibaar-backend:latest
    container_name: borsibaar-backend
    expose:
      - "8080"
    env_file:
      - .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS}
    depends_on:
      - postgres
    restart: unless-stopped
    networks:
      - borsibaar-network

  frontend:
    image: janx4u/borsibaar-frontend:latest
    container_name: borsibaar-frontend
    expose:
      - "3000"
    environment:
      BACKEND_URL: http://backend:8080
      NEXT_PUBLIC_BACKEND_URL: ${NEXT_PUBLIC_BACKEND_URL}
    depends_on:
      - backend
    restart: unless-stopped
    networks:
      - borsibaar-network

  nginx:
    image: nginx:alpine
    container_name: borsibaar-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - frontend
      - backend
    restart: unless-stopped
    networks:
      - borsibaar-network

volumes:
  postgres_data:

networks:
  borsibaar-network:
    driver: bridge
```

Faili loomise käsk serveris (kopeeri täpselt, `EOF` peab olema rea alguses ilma tühikuteta):

```bash
cat > docker-compose.yml << 'EOF'
version: "3.3"
services:
  postgres:
    image: postgres:17
    container_name: borsibaar-db
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - borsibaar-network

  backend:
    image: janx4u/borsibaar-backend:latest
    container_name: borsibaar-backend
    expose:
      - "8080"
    env_file:
      - .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS}
    depends_on:
      - postgres
    restart: unless-stopped
    networks:
      - borsibaar-network

  frontend:
    image: janx4u/borsibaar-frontend:latest
    container_name: borsibaar-frontend
    expose:
      - "3000"
    environment:
      BACKEND_URL: http://backend:8080
      NEXT_PUBLIC_BACKEND_URL: ${NEXT_PUBLIC_BACKEND_URL}
    depends_on:
      - backend
    restart: unless-stopped
    networks:
      - borsibaar-network

  nginx:
    image: nginx:alpine
    container_name: borsibaar-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - frontend
      - backend
    restart: unless-stopped
    networks:
      - borsibaar-network

volumes:
  postgres_data:

networks:
  borsibaar-network:
    driver: bridge
EOF
```

Loo lihtne `nginx.conf` reverse proxy jaoks:

```nginx
upstream backend {
    server backend:8080;
}

upstream frontend {
    server frontend:3000;
}

server {
    listen 80;

    # Auth ja OAuth endpointid -> backend
    location /auth/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Kõik muu -> frontend
    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

Faili loomise käsk serveris (kui esineb viga `Is a directory`, eemalda kõigepealt kaust):

```bash
rm -rf nginx.conf

cat > nginx.conf << 'EOF'
upstream backend {
    server backend:8080;
}

upstream frontend {
    server frontend:3000;
}

server {
    listen 80;

    location /auth/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF
```

> **Miks NGINX?** Frontend (`localhost:3000`) ja backend (`localhost:8080`) on erinevatel portidel.
> NGINX toimib reverse proxy-na – kasutaja näeb ainult porti 80, aga NGINX suunab päringud õigesse konteinerisse.
> See lahendab ka CORS probleemi, sest kõik päringud tulevad samalt domeenilt.

Käivita:

```bash
docker compose up -d

# Kontrolli, et kõik konteinerid töötavad
docker compose ps

# Vaata logisid
docker compose logs -f
```

Kontrolli brauseris: `http://193.40.157.110/`

### 2.4 Korrastamine (kui teised tahavad harjutada)

```bash
docker compose down
```

---

## Samm 3 – Domeen

### 3.1 Tasuta domeeni registreerimine (noip.com)

1. Mine [noip.com](https://www.noip.com) ja registreeru
2. Vali "Create Hostname"
3. Sisesta domeeninimi (nt `borsibaar.zapto.org`)
4. Tüüp: **A Record**
5. IP aadress: sinu serveri IP

Meie projektis on kasutusel `borsibaar.zapto.org` (näed seda `nginx/conf.d/borsibaar-https.conf` failis).

### 3.2 Kontrolli, et domeen töötab

```bash
# Serverist
ping borsibaar.zapto.org

# Või DNS kontrolli
nslookup borsibaar.zapto.org
```

Nüüd peaks rakendus olema ligipääsetav aadressil: `http://sinudomeen.zapto.org/`

---

## Samm 4 – CI/CD (Pidev Integratsioon ja Deploy)

CI/CD automatiseerib protsessi: kood pushitakse → ehitatakse → deployitakse serverisse.

Meie workflow asub: `.github/workflows/docker-image.yml`

### 4.1 GitHub Secrets seadistamine

Mine GitHub repos: **Settings → Secrets and variables → Actions → New repository secret**

Lisa järgmised saladused:

| Secret nimi | Väärtus |
|---|---|
| `SSH_USER` | Sinu SSH kasutajanimi serveris |
| `SSH_PRIVATE_KEY` | Sinu SSH privaatvõti (sisu `~/.ssh/id_rsa` failist) |
| `SERVER_IP` | Serveri IP aadress |
| `ENV_PRODUCTION_FILE` | Kogu `.env` faili sisu (kõik muutujad) |

**SSH võtme genereerimine (kui sul veel pole):**

```bash
# Genereeri võtmepaar
ssh-keygen -t ed25519 -C "github-actions"

# Lisa avalik võti serverisse (et GitHub Actions saaks sisse logida)
ssh-copy-id -i ~/.ssh/id_ed25519.pub kasutaja@SERVER_IP

# Kopeeri privaatvõti GitHub Secret'isse
cat ~/.ssh/id_ed25519
```

### 4.2 Kuidas workflow töötab

Meie `.github/workflows/docker-image.yml` teeb järgmist, kui kood pushitakse `main` harusse:

```
1. Checkout kood
2. Seadista JDK 21
3. Ehita backend (mvn clean package)
4. Seadista Node.js 20
5. Installi frontend sõltuvused (npm ci)
6. Ehita frontend (npm run build)
7. Seadista SSH võti
8. Paki kõik deployment.tar.gz paketti
9. Kopeeri pakett serverisse (scp)
10. SSH kaudu serveris:
    - Paki lahti
    - docker compose down
    - docker compose build --no-cache
    - docker compose up -d
```

### 4.3 Workflow käivitamine

```bash
# Pushige kood main harusse
git push origin main
```

Mine GitHub → Actions tab → näed workflow jooksmas.

### 4.4 Bonus: Docker Registry kasutamine (soovituslik)

Praegune lähenemine ehitab image'id serveris. Parem lähenemine:
1. Ehita image CI runneris
2. Push image registrisse (Docker Hub, GHCR)
3. Server ainult **pulb** image'i (ei ehita)

See võimaldab lihtsat rollback'i:

```bash
# Tagasi minemiseks vana versiooni juurde
docker pull sinunimi/borsibaar-backend:v1.0.2
docker compose up -d
```

---

## Samm 5 – HTTPS (TLS sertifikaat)

HTTPS krüpteerib liikluse serveri ja brauseri vahel. Let's Encrypt annab tasuta sertifikaate.

### 5.1 Certboti installimine serveris

```bash
sudo apt-get install -y certbot
```

### 5.2 Sertifikaadi hankimine

**Oluline:** NGINX peab olema maha võetud enne sertifikaadi hankimist (port 80 peab vaba olema), VÕI kasuta Certboti NGINX pluginat.

**Variant A – Standalone (lihtsaim):**

```bash
# Peata NGINX ajutiselt
docker compose stop nginx

# Hangi sertifikaat
sudo certbot certonly --standalone -d sinudomeen.zapto.org

# Sertifikaadid on nüüd:
# /etc/letsencrypt/live/sinudomeen.zapto.org/fullchain.pem
# /etc/letsencrypt/live/sinudomeen.zapto.org/privkey.pem
```

**Variant B – Webroot (NGINX töötab edasi):**

```bash
sudo certbot certonly --webroot \
  -w /var/www/certbot \
  -d sinudomeen.zapto.org
```

### 5.3 SSL sertifikaatide kasutamine Docker Compose'is

Kopeeri sertifikaadid projekti kausta:

```bash
mkdir -p /opt/sinuNimi/borsibaar/nginx/ssl
sudo cp /etc/letsencrypt/live/sinudomeen.zapto.org/fullchain.pem /opt/sinuNimi/borsibaar/nginx/ssl/
sudo cp /etc/letsencrypt/live/sinudomeen.zapto.org/privkey.pem /opt/sinuNimi/borsibaar/nginx/ssl/
sudo cp /etc/letsencrypt/live/sinudomeen.zapto.org/chain.pem /opt/sinuNimi/borsibaar/nginx/ssl/
```

Meie `docker-compose.prod.yaml` mountib need sertifikaadid NGINX konteinerisse:

```yaml
nginx:
  volumes:
    - ./nginx/ssl:/etc/nginx/ssl:ro
```

Ja `nginx/conf.d/borsibaar-https.conf` kasutab neid:

```nginx
ssl_certificate /etc/nginx/ssl/fullchain.pem;
ssl_certificate_key /etc/nginx/ssl/privkey.pem;
```

Lisa docker-compose'i NGINX konfiguratsioonile port 443 ja HTTPS server blokk (vaata `nginx/conf.d/borsibaar-https.conf` täielikku näidet).

### 5.4 Sertifikaadi uuendamine

Let's Encrypt sertifikaadid kehtivad 90 päeva. Automaatne uuendamine:

```bash
# Testi uuendamist
sudo certbot renew --dry-run

# Lisa cron job automaatseks uuendamiseks
sudo crontab -e
# Lisa rida:
# 0 12 * * * /usr/bin/certbot renew --quiet
```

---

## Kokkuvõte

| Samm | Tulemus |
|---|---|
| 1. Serveri seadistamine | SSH ligipääs, Docker ja NGINX serveris |
| 2. Manuaalne deploy | Rakendus töötab `http://SERVER_IP/` |
| 3. Domeen | Rakendus töötab `http://sinudomeen.zapto.org/` |
| 4. CI/CD | Iga push `main` harusse deployib automaatselt |
| 5. HTTPS | Rakendus töötab `https://sinudomeen.zapto.org/` |

---

## Kasulikud käsud

```bash
# Konteinerite olek
docker compose ps

# Logid (kõik konteinerid)
docker compose logs -f

# Konkreetse konteineri logid
docker compose logs -f backend

# Konteinerite taaskäivitus
docker compose restart

# Kõik maha
docker compose down

# Kõik maha + volumes (kustutab andmebaasi!)
docker compose down -v

# SSH tunnel andmebaasile (lokaalsel masinal)
ssh -L 5432:localhost:5432 kasutaja@SERVER_IP
```

---

## Levinud probleemid

**Problem: `docker: permission denied`**
```bash
sudo usermod -aG docker $USER
# Logi uuesti sisse
```

**Problem: Port 80 on juba hõivatud**
```bash
sudo lsof -i :80
sudo systemctl stop nginx  # kui NGINX töötab süsteemi tasemel
```

**Problem: Backend ei saa andmebaasiga ühenduda**
```bash
# Kontrolli, et postgres konteiner töötab
docker compose ps postgres
# Vaata logisid
docker compose logs postgres
```

**Problem: Frontend ei näe backendi**
```bash
# Frontend kasutab Docker network nime 'backend', mitte localhost
# Kontrolli .env muutujat BACKEND_URL=http://backend:8080
docker compose logs frontend
```

**Problem: Maven build ebaõnnestub – `AccessDeniedException: /home/.../.m2/repository`**
```bash
# .m2 kaust kuulub root-ile (tekib kui varem käivitati sudo-ga)
sudo chown -R $USER:$USER ~/.m2
# Seejärel proovi uuesti
./mvnw clean package -DskipTests
```
