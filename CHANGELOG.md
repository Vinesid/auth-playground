# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.2.0] - 2016-10-13

### Added
- Configurable validity period: Users will be set inactive when not logging in once in this period. 
 - _**validate-user-login**_ receives optional _validity-period-in-ms_
 - _**get-users**_ can be called with additional parameter to get active information for each user
 - _**active-user?**_ checks if user is still active
 - _**activate-user**_ & _**deactivate-user**_ 
 
## [0.1.0] - 2016-08-21