.PHONY: test lint format-check format ci uber clean

test:
	clojure -M:test

lint:
	clojure -M:lint

format-check:
	clojure -M:format-check

format:
	clojure -M:format-fix

ci: format-check lint test

uber:
	clojure -T:build uber

clean:
	clojure -T:build clean
