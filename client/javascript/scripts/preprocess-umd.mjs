import fs from "fs";

const srcDir = "src/main/javascript";
const workDir = "target/temp";
const markerBegin = "// preprocess-umd: BEGIN";
const markerEnd = "// preprocess-umd: END";

fs.mkdir(workDir, _err => {/* ignore */});

fs.readdir(srcDir, (_err, files) => {
    files.forEach(file => {
        if (file.endsWith(".test.js")) {
            return;
        }
        console.log("Preprocess", file);
        const source = fs.readFileSync(`${srcDir}/${file}`, "utf-8");
        const processed = source.split(markerBegin)[1]?.split(markerEnd)[0] ?? source;
        fs.writeFileSync(`${workDir}/${file}`, processed);
    });
});
