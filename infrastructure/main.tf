provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source                  = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                    = "unspec-${var.env}"
  product                 = var.product
  env                     = var.env
  tenant_id               = var.tenant_id
  object_id               = var.jenkins_AAD_objectId
  resource_group_name     = azurerm_resource_group.rg.name
  product_group_object_id = "40c33f5a-24d0-4b22-a923-df8a80a59cd9"
  common_tags             = var.common_tags
  create_managed_identity = true
}

locals {
  vaultName = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "damages_key_vault" {
  name = local.vaultName
  resource_group_name = "${var.product}-service-${var.env}"
}

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.component}-appinsights-${var.env}"
  location            = var.appinsights_location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
}

data "azurerm_key_vault" "send_grid" {
  provider = azurerm.send-grid

  name                = var.env != "prod" ? "sendgridnonprod" : "sendgridprod"
  resource_group_name = var.env != "prod" ? "SendGrid-nonprod" : "SendGrid-prod"
}

data "azurerm_key_vault_secret" "send_grid_api_key" {
  provider = azurerm.send-grid


  key_vault_id = data.azurerm_key_vault.send_grid.id
  name         = "hmcts-damages-api-key"
}

resource "azurerm_key_vault_secret" "sendgrid_api_key" {
  key_vault_id = data.azurerm_key_vault.damages_key_vault.id
  name         = "sendgrid-api-key"
  value        = data.azurerm_key_vault_secret.send_grid_api_key.value
}
