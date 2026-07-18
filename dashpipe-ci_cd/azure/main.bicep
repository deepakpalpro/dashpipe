@description('Dashpipe Azure Wave A — ACR + AKS (+ optional Wave B MySQL/Service Bus)')
param namePrefix string = 'dashpipe'
param location string = resourceGroup().location

@description('AKS node VM size. B-series often blocked; D2s_v7 is widely available in eastus.')
param aksVmSize string = 'Standard_D2s_v7'

@description('System node pool count (1 is enough for empty Wave A).')
param aksNodeCount int = 1

@description('When true, also create Azure MySQL Flexible + Service Bus (Wave B). Wave A uses in-cluster MySQL/RabbitMQ.')
param deployManagedDataPlane bool = false

param mysqlAdminLogin string = 'dashpipeadmin'

@secure()
@description('Required only when deployManagedDataPlane=true')
param mysqlAdminPassword string = ''

var acrName = replace('${namePrefix}acr${uniqueString(resourceGroup().id)}', '-', '')
var aksName = '${namePrefix}-aks'
var mysqlName = '${namePrefix}-mysql'
// Service Bus forbids suffix "-sb"
var serviceBusName = '${namePrefix}-servicebus'
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

resource serviceBus 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = if (deployManagedDataPlane) {
  name: serviceBusName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
  }
}

resource mysql 'Microsoft.DBforMySQL/flexibleServers@2023-12-30' = if (deployManagedDataPlane) {
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
        count: aksNodeCount
        vmSize: aksVmSize
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
output acrName string = acr.name
output aksName string = aks.name
output logAnalyticsWorkspaceId string = logAnalytics.id
output mysqlFqdn string = deployManagedDataPlane ? mysql!.properties.fullyQualifiedDomainName : ''
output serviceBusEndpoint string = deployManagedDataPlane ? serviceBus!.properties.serviceBusEndpoint : ''
