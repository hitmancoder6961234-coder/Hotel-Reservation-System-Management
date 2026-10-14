#!/usr/bin/env bash
# ============================================================================
#  One-time preparation of a fresh Ubuntu 22.04 / 24.04 server for StayEase.
#  Run as a sudo-capable user (not necessarily root):   bash scripts/ubuntu-setup.sh
#
#  1. installs Docker Engine + the Compose plugin from Docker's official apt repository
#  2. adds the current user to the `docker` group
#  3. configures the UFW firewall: SSH (22), HTTP (80), HTTPS (443) only
#
#  It does NOT touch other running sites. Read it before you run it.
#  If Docker is already installed on the server, skip step 1 (do not run this script).
# ============================================================================
set -euo pipefail

if [ "$(id -u)" -eq 0 ]; then SUDO=""; else SUDO="sudo"; fi

echo "==> Installing Docker Engine and the Compose plugin"
$SUDO apt-get update
$SUDO apt-get install -y ca-certificates curl gnupg git
$SUDO install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | $SUDO gpg --dearmor --yes -o /etc/apt/keyrings/docker.gpg
$SUDO chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | $SUDO tee /etc/apt/sources.list.d/docker.list >/dev/null
$SUDO apt-get update
$SUDO apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
$SUDO systemctl enable --now docker

if [ "$(id -u)" -ne 0 ]; then
  $SUDO usermod -aG docker "$USER"
  echo "    (log out and back in once so the 'docker' group applies)"
fi

echo "==> Firewall (UFW): allow SSH, HTTP, HTTPS - everything else stays closed"
$SUDO apt-get install -y ufw
$SUDO ufw allow OpenSSH          # BEFORE enabling, so you cannot lock yourself out
$SUDO ufw allow 80/tcp
$SUDO ufw allow 443/tcp
$SUDO ufw --force enable
$SUDO ufw status verbose

echo
echo "Note: Docker publishes container ports by editing iptables directly, which bypasses UFW rules."
echo "That is why compose.yaml publishes ONLY nginx (80/443) and never MySQL or Tomcat."
echo "Done. Next: follow docs/DEPLOYMENT.md (clone, .env, certificate, docker compose up -d --build)."
