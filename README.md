# healthcheck

Healthchecks provide insights about a platform's health and can help trigger alerts or pay attention to key components that might need close attention. Jahia's Healthcheck module provides a JSON output and is can be triggered at will with minimal impact on the platform load.

The healthcheck module is a core component that can be used in conjunction with extension modules in order to provide more information to the monitoring systems.

## Usage

The healthcheck is available thourgh the servlet `/healthcheck` to all users who are granted the Jahia DX server role `monitoring`
It returns a JSON object with the following structure:

