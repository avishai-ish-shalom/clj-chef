# clj-chef

This is an API library for talking to [Opscode Chef](http://www.opscode.com/chef/) servers.
Although this library is already usable it is still rough around the edges and needs more work. Contributors welcome!

Most of the chef server API is covered, some calls intentionally omitted because they are only used by chef-client/solo and not by knife. Also omitted `cookbook-upload` because it requires parsing ruby files to impelement correctly.

The project uses [Midje](https://github.com/marick/Midje/).

## How to use
Unlike other Chef libraries, this library uses [edn](https://github.com/edn-format/edn) file for its config.
Currently the config is something like:

	{
		:server-url "https://api.opscode.com/"
		:client-key "/home/user/.chef/user-name.pem"
		:client-name "user-name"
		:organization "organization"
	}

`clj-chef.core` has a convience method for loading the config and the client key in one step:

	(def conf (clj-chef.core/read-config "/path/to/config.edn"))

After loading the config you can use the `with-config` macro with `clj-chef.api_client` functions:

	(require '[clj-chef.api_client :as chef])
	(chef/with-config conf (chef/role-list))

API functions follow the naming scheme of knife, e.g. node-list, node-create, node-delete, etc.
Currently functions return raw maps representing Chef objects.

The search function follows the ruby form (more or less):

	(chef/with-config conf (chef/search :node "*:*"))

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

## License
This library is licensed under the LGPL v3 license. See LICENSE file for more details