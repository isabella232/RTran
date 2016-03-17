# Contributing to RTran

Contributions via GitHub pull requests are gladly accepted from their original author. 
Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. 
Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

Please use the following steps to determine the path of contribution.

## Proposals/Discussions

We all should discuss new ideas before they turn into code. 
If you have any proposals, please create an issue in issue tracker and tag it as 'Proposal'.
Once we get to a certain conclusion, we can separate the work and start coding. 

## Bugs

If you have determined you are facing a bug or defect, please log the bug in the issue tracker and tag it as 'Bug'. 
If the bug has any reference to proposals, please add the reference to the proposal.

## Contribution Process

The standard way of contributing ideas/features and bug fixes is by pull requests.

* Make sure you have an active github account.
* [Fork](https://help.github.com/articles/fork-a-repo/) the RTran repo into your account.
* Make modifications to the master branch. If the contribution is a bug fix that needs to go into a release branch, please provide that as a comment on the pull request (below).
* Provide test cases and ensure the test coverage of the added code and functionality.
* If the change impacts documentation, please provide documentation fixes or additional documentation for the feature.
* Commit and push your contributions. The commit message must reference the issue solved by this commit.
* Please [squash](https://github.com/edx/edx-platform/wiki/How-to-Rebase-a-Pull-Request) multiple commits for a single feature and bug fix into a single commit.
* Make a [pull request](https://help.github.com/articles/using-pull-requests/). For bug fixes that need to go into a current release, please note so in the pull request comments.
* We'll review your pull requests and use github to communicate comments. Please ensure your email account in github is accurate as it will be used to communicate review comments.
* If modifications are required, please [squash](https://github.com/edx/edx-platform/wiki/How-to-Rebase-a-Pull-Request) your commits after making the modifications to update the pull request.
* We merge the pull request after successful submission and review, and close the issue. In case of bug fixes that need to go into a current release branch, we'll do the proper cherry-pick.
* Upon successful merge and regression tests, the SNAPSHOT artifact reflecting the change will be published on [maven central snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/).

## Best Practice

Generally, it is a good practice to reflect a single issue in a pull request. Multiple issues fixed by a single
pull request will be accepted **only if** these cannot be separated into individual commits and individual pull requests
reflecting each issue separately. Every pull request **MUST** contains the issue reference.

## Rule Implementations

If you implemented your own rules and you think those rules are generic for a certain type of project, you are absolutely welcome to contribute back as an entire module.
Still, you can also open source the module in your own repository. Just make sure to license your work under the project's open source license.

Thank you very much in advance!