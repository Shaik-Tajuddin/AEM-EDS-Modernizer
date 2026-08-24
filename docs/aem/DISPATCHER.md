# Dispatcher Configuration

The Apache HTTP Server + Dispatcher config for serving the
modernizer dashboard and proxying the API back to AEM
Author.

## Files

- `dispatcher/src/conf.d/available_vhosts/aem-eds-modernizer.vhost`
  — the vhost that serves the dashboard.
- `dispatcher/src/conf.dispatcher.d/available_farms/aem-eds-modernizer.farm`
  — the farm that defines the caching and routing rules.

## Vhost

```apache
<VirtualHost *:80>
  ServerName aem-eds-modernizer.example.com
  DocumentRoot /var/www/aem-eds-modernizer

  <Directory />
      Require all denied
  </Directory>

  <Directory /var/www/aem-eds-modernizer>
      Require all granted
  </Directory>

  # SPA HTML: cached for 1 day
  <LocationMatch "^/bin/aem-eds-modernizer/index\.html$">
    Header set Cache-Control "max-age=86400, public"
  </LocationMatch>

  # SPA assets: cached for 1 year (immutable)
  <LocationMatch "^/bin/aem-eds-modernizer/(styles|scripts)/.+\.(css|js)$">
    Header set Cache-Control "max-age=31536000, immutable"
  </LocationMatch>

  # API: never cached
  <LocationMatch "^/bin/aem-eds-modernizer/api/">
    Header set Cache-Control "no-store"
    Header set X-Content-Type-Options "nosniff"
    ProxyPass https://author-pXXXX-eYYYY.adobeaemcloud.com
    ProxyPassReverse https://author-pXXXX-eXXXX.adobeaemcloud.com
  </LocationMatch>
</VirtualHost>
```

## Farm

```apache
/authorfarm {
  /clientheaders {
    "Accept"
    "Authorization"
    "Cache-Control"
    "Content-Type"
    "If-Match"
    "If-Modified-Since"
    "If-None-Match"
    "X-Requested-With"
  }
  /filters {
    /0001 { /type "allow" /glob "*" }
  }
  /cache {
    /rules {
      /0001 { /glob "/bin/aem-eds-modernizer/*" /type "allow" }
    }
  }
  /renders {
    /0001 { /hostname "author-pXXXX-eYYYY.adobeaemcloud.com" /port "443" }
  }
  /statistics { /categories { } }
  /virtualhosts {
    /0001 { /address "aem-eds-modernizer.example.com" }
  }
}
```

## Caching strategy

| Path | Cache |
|---|---|
| `/bin/aem-eds-modernizer/index.html` | 1 day, public |
| `/bin/aem-eds-modernizer/{styles,scripts}/*` | 1 year, immutable |
| `/bin/aem-eds-modernizer/api/*` | `no-store` |
| `/bin/aem-eds-modernizer/api/events` | `no-store` (polled by dashboard) |

## Related

- [DEPLOY.md](DEPLOY.md) — full deployment runbook.
- [OSGI_CONFIG.md](OSGI_CONFIG.md) — OSGi configuration.
