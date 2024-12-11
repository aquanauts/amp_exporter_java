SHELL:=$(shell which bash)

.PHONY: help
help:
	@grep -hE '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

ifndef VERBOSE
.SILENT:
endif

.PHONY: spotless
spotless:
	git clean -fX build
	git clean -fX .gradle
	./gradlew clean

.PHONY: test
test: ## Run CI Checks
	./gradlew cleanTest --warning-mode all test --continue spotlessCheck

.PHONY: watch
watch: ## Run tests continuously
	./gradlew -t test --continue

.PHONY: todo
todo: # Print a list of relevant TODOs in the code
	@echo "Remaining WIP:"
	git grep -n TODO | grep -v README.md | egrep -v '^Makefile.*:todo:' && exit 1 || exit 0

.PHONY: reformat
reformat: ## Reformat all the code (as per pre-commit)
	./gradlew spotlessApply

publish: test
	./gradlew publish