@description('Dashflow Azure assembly — AKS, MySQL, Service Bus, ACR, Log Analytics')
param namePrefix string = 'dashflow'
param location string = resourceGroup().location
param mysqlAdminLogin string = 'dashflowadmin'
@secure()
param mysqlAdminPassword string

var acrName = replace('${namePrefix}acr${uniqueString(resourceGroup().id)}', '-', '')
var aksName = '${namePrefix}-aks'
var mysqlName = '${namePrefix}-mysql'
var sbName = '${namePrefix}-sb'
var lawName = '${namePrefix}-law'

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: lawName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: toLower(take(acrName, 50))
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
  }
}

resource serviceBus 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = {
  name: sbName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
  }
}

resource mysql 'Microsoft.DBforMySQL/flexibleServers@2023-12-30' = {
  name: mysqlName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '8.0.21'
    administratorLogin: mysqlAdminLogin
    administratorLoginPassword: mysqlAdminPassword
    storage: {
      storageSizeGB: 20
    }
    backup: {
      backupRetentionDays: 7
    }
  }
}

resource aks 'Microsoft.ContainerService/managedClusters@2024-01-01' = {
  name: aksName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    dnsPrefix: take('${namePrefix}aks', 40)
    agentPoolProfiles: [
      {
        name: 'agentpool'
        count: 2
        vmSize: 'Standard_B2s'
        mode: 'System'
        osType: 'Linux'
      }
    ]
    addonProfiles: {
      omsagent: {
        enabled: true
        config: {
          logAnalyticsWorkspaceResourceID: logAnalytics.id
        }
      }
    }
  }
}

output acrLoginServer string = acr.properties.loginServer
output aksName string = aks.name
output mysqlFqdn string = mysql.properties.fullyQualifiedDomainName
output serviceBusEndpoint string = serviceBus.properties.serviceBusEndpoint
output logAnalyticsWorkspaceId string = logAnalytics.id
