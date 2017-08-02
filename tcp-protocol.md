# Command Reference: JTradfri TCP Socket

This is a list of commands that are available on the TCP socket of the JTradfri software.

The TCP socket listens on port 1505 by default. This port may be changed by the `-p` parameter on the command line call of the software.
Only connections from localhost are allowed by default

Both commands and responses are terminated by the newline character.

## Commands for devices

### `device::list`
This lists all devices that are connected to the gateway. Currently, it only lists the IDs (not the device's names).
The output looks like this: `deviceList::[65539, 65537, 65538]`.

### `device::update::<device-id>`
This forces the software to update the values of a device. It returns nothing.
Replace device-id with the appropriate device-id of the device you want to update.

### `device::subscribe::<device-id>`
This enables observation of the state of a device. The command itself returns nothing.

Once there was a change in any values of the device a message will be send:
```
subscribedDeviceUpdate::<device-id>::<device-json-info>
```

### `device::info::<device-id>`
This returns the current buffered info of a device. Please note, that this info isn't necessarily up-to-date. The buffer either gets updated by `device::update::<device-id>` or if something changes for the device and a device subscription is active.

It returns the following:
```
deviceInfo::<device-id>::<device-json-info>
```

### `device::set::<device-id>::<attribute>::<value>`

This writes an attribute to a specified device. This command returns nothing.

The following attributes can be set:


| Attribute |       Values       |                                   Description                                  |
|:---------:|:------------------:|:------------------------------------------------------------------------------:|
| onoff     | 0 or 1             | Turns the device on or off                                                     |
| color     | 6 digit hex number | Sets the light color of the device. The most devices only support some colors. |
| dimvalue  | 0 - 254            | Sets the brightness of the device                                              |
| name      | any string         | Sets the name of the device                                                    |

### Device JSON Info
The JSON Info that is returned after subscription or an info-command looks like this, but in a single line (without whitespaces):

```
{
   "lastSeenAt":1501407261,
   "createdAt":1492280964,
   "reachabilityState":1,
   "name":"Fenster Links",
   "dimvalue":200,
   "type":"TRADFRI bulb E27 opal 1000lm",
   "deviceid":65537,
   "version":"1.1.1.0-5.7.2.0",
   "manufacturer":"IKEA of Sweden",
   "onoff":0
}
```

## Commands for groups

### `group::list`
This lists all groups that are configured in the gateway. Currently, it only lists the IDs (not the group's names).
The output looks like this: `groupList::[196210, 173540]`.

### `group::update::<group-id>`
This forces the software to update the values of a group. It returns nothing.
Replace group-id with the appropriate group-id of the group you want to update.

### `group::subscribe::<group-id>`
This enables observation of the state of a group. The command itself returns nothing.

Once there was a change in any values of the device a message will be send:
```
subscribedGroupUpdate::<group-id>::<group-json-info>
```

### `group::info::<group-id>`
This returns the current buffered info of a group. Please note, that this info isn't necessarily up-to-date. The buffer either gets updated by `group::update::<group-id>` or if something changes for the group and a group subscription is active.

It returns the following:
```
groupInfo::<group-id>::<group-json-info>
```

### `group::set::<group-id>::<attribute>::<value>`

This writes an attribute to a specified group. This command returns nothing.

The following attributes can be set:


| Attribute |       Values       |                                   Description                                  |
|:---------:|:------------------:|:------------------------------------------------------------------------------:|
| onoff     | 0 or 1             | Turns the device on or off                                                     |
| mood      | mood id            | Set the mood of the group                                                      |
| dimvalue  | 0 - 254            | Sets the brightness of the device                                              |
| name      | any string         | Sets the name of the device                                                    |

### Group JSON Info
The JSON Info that is returned after subscription or an info-command looks like this, but in a single line (without whitespaces):

```
{
   "createdAt":1494088484,
   "mood":198884,
   "groupid":173540,
   "members":[
      {
         "name":"Fenster Links",
         "deviceid":65537
      },
      {
         "deviceid":65536
      },
      {
         "name":"Fenster Rechts",
         "deviceid":65538
      }
   ],
   "name":"Wohnzimmer",
   "dimvalue":200,
   "onoff":0
}
```