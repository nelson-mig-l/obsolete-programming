import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

class Solution {

    static class ValueStack extends Stack<Integer> {
    }

    static class Instructions extends ArrayList<Instruction> {
        public Instructions(Instructions instructions) {
            super(instructions);
        }

        public Instructions() {
            super();
        }
    }

    static class FunctionTable extends HashMap<String, Instructions> {
    }

    static class Context {
        private final ValueStack stack;
        private final FunctionTable table;
        private final IO io;

        Context(ValueStack stack, FunctionTable table, IO io) {
            this.stack = stack;
            this.table = table;
            this.io = io;
        }
    }

    interface InstructionBody extends Consumer<Context> {
    }

    static class Instruction {
        final String name;
        final InstructionBody body;

        Instruction(String name, InstructionBody body) {
            this.name = name;
            this.body = body;
        }

        void execute(Context ctx) {
            body.accept(ctx);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    interface InstructionFactory extends Function<Parser, Instruction> {
    }

    static class Tokens implements Iterator<String> {

         Iterator<String> lineIterator;
         Iterator<String> tokenIterator;


        Tokens(final List<String> lines) {
            lineIterator = lines.iterator();
            tokenizeNextLine();
        }

        @Override
        public boolean hasNext() {
            return tokenIterator.hasNext() || lineIterator.hasNext();
        }

        @Override
        public String next() {
            if (!tokenIterator.hasNext()) {
                tokenizeNextLine();
            }
            return tokenIterator.next();
        }

        private void tokenizeNextLine() {
            tokenIterator = Arrays.asList(lineIterator.next().trim().split(" ")).iterator();
        }
    }

    enum InstructionSet {
        ADD(parser ->
                new Instruction("Add", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v0 + v1);
                })
        ),
        SUB(parser ->
                new Instruction("Sub", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v0 - v1);
                })
        ),
        MUL(parser ->
                new Instruction("Mul", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v0 * v1);
                })
        ),
        DIV(parser ->
                new Instruction("Div", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v0 / v1);
                })
        ),
        MOD(parser ->
                new Instruction("Mod", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v0 % v1);
                })
        ),
        POP(parser ->
                new Instruction("Pop", ctx -> ctx.stack.pop())
        ),
        DUP(parser ->
                new Instruction("Dup", ctx -> {
                    final int v0 = ctx.stack.peek();
                    ctx.stack.push(v0);
                })
        ),
        SWP(parser ->
                new Instruction("Swp", ctx -> {
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v1);
                    ctx.stack.push(v0);
                })
        ),
        ROT(parser ->
                new Instruction("Rot", ctx -> {
                    final int v2 = ctx.stack.pop();
                    final int v1 = ctx.stack.pop();
                    final int v0 = ctx.stack.pop();
                    ctx.stack.push(v1);
                    ctx.stack.push(v2);
                    ctx.stack.push(v0);
                })
        ),
        OVR(parser ->
                new Instruction("Ovr", ctx -> {
                    final int v0 = ctx.stack.pop();
                    final int v1 = ctx.stack.peek();
                    ctx.stack.push(v0);
                    ctx.stack.push(v1);
                })
        ),
        POS(parser ->
                new Instruction("Pos", ctx -> {
                    final int value = ctx.stack.pop();
                    ctx.stack.push(value >= 0 ? 1 : 0);
                })
        ),
        NOT(parser ->
                new Instruction("Not", ctx -> {
                    final int value = ctx.stack.pop();
                    ctx.stack.push(value == 0 ? 1 : 0);
                })
        ),
        OUT(parser ->
                new Instruction("Out", ctx -> {
                    final int v0 = ctx.stack.pop();
                    ctx.io.write(v0);
                })
        ),
        IN(parser ->
                new Instruction("In", ctx -> {
                    final int value = ctx.io.read();
                    ctx.stack.push(value);
                })
        ),
        DEF(parser -> {
            final String name = parser.tokens.next();
            parser.beginBlock();
            return new Instruction("Def " + name, ctx -> {
            });
        }),
        END(parser -> {
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            final Instruction def = instructions.remove(0);
            final String name = def.name.split(" ")[1]; // HACK
            return new Definition(name, instructions);
        }),
        IF(parser -> {
            parser.beginBlock();
            return new Instruction("If", ctx -> {
            });
        }),
        ELS(parser -> {
            parser.beginBlock();
            return new Instruction("Els", ctx -> {
            });
        }),
        FI(parser -> {
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            final Instruction ifOrElse = instructions.remove(0);
            if (ifOrElse.name.equals("Els")) {
                final Instructions ifInstructions = parser.getInstructions();
                parser.endBlock();
                ifInstructions.remove(0);
                return new Conditional(ifInstructions, instructions);
            } else {
                return new Conditional(instructions);
            }
        });

        static class Call extends Instruction {
            Call(String name) {
                super("Call(" + name + ")", ctx -> {
                    if (ctx.table.containsKey(name)) {
                        ctx.table.get(name).forEach(instruction -> instruction.execute(ctx));
                    } else {
                        throw new RuntimeException(name);
                    }
                });
            }
        }

        static class Push extends Instruction {
            Push(final String value) {
                super("Push(" + value + ")", ctx -> ctx.stack.push(Integer.parseInt(value)));
            }
        }

        static class Definition extends Instruction {
            private final Instructions instructions;

            Definition(String name, Instructions instructions) {
                super("Def(" + name + ")", ctx -> ctx.table.put(name, instructions));
                this.instructions = instructions;
            }

            @Override
            public String toString() {
                return super.toString() + "\n\t" + instructions.toString();
            }
        }

        static class Conditional extends Instruction {
            private final Instructions affirmative;
            private final Instructions zero;

            Conditional(Instructions positive) {
                this("If", positive, new Instructions());
            }

            Conditional(Instructions affirmative, Instructions zero) {
                this("IfEls", affirmative, zero);
            }

            private Conditional(String name, Instructions affirmative, Instructions zero) {
                super(name, (ctx) -> {
                    if (ctx.stack.pop() == 0) {
                        zero.forEach(i -> i.execute(ctx));
                    } else {
                        affirmative.forEach(i -> i.execute(ctx));
                    }
                });
                this.affirmative = affirmative;
                this.zero = zero;
            }

            @Override
            public String toString() {
                return super.toString() + affirmative.toString() + (zero.isEmpty() ? "" : "," + zero);
            }

        }

        final InstructionFactory factory;

        InstructionSet(final InstructionFactory factory) {
            this.factory = factory;
        }

        static Optional<Instruction> get(final String token, final Parser parser) {
            return Arrays.stream(InstructionSet.values())
                    .filter(v -> v.name().equals(token))
                    .findAny()
                    .map(v -> v.factory.apply(parser));
        }

    }

    static class Parser {
        private final Tokens tokens;
        private final Stack<Instructions> instructionStack = new Stack<>();

        private Instructions instructions;

        Parser(final Tokens tokens) {
            this.tokens = tokens;
        }

        void parse(final String token) {
            if (Character.isAlphabetic(token.codePointAt(0))) {
                final Optional<Instruction> instruction = InstructionSet.get(token, this);
                this.addInstruction(instruction.isEmpty()
                        ? new InstructionSet.Call(token)
                        : instruction.get()
                );
            } else {
                this.addInstruction(new InstructionSet.Push(token));
            }
        }


        void addInstruction(Instruction instruction) {
            instructions.add(instruction);
        }

        Instructions getInstructions() {
            return new Instructions(instructions);
        }

        void beginBlock() {
            instructionStack.push(instructions);
            this.instructions = new Instructions();
        }

        // Before this getInstructions is always called
        void endBlock() {
            this.instructions = instructionStack.pop();
        }

    }

    static class IO {
        private final Scanner in;
        private final PrintStream out;
        public IO(Scanner in, PrintStream out) {
            this.in = in;
            this.out = out;
        }

        int read() {
            return in.nextInt();
        }

        void write(int value) {
            out.println(value);
        }
    }

    static class Interpreter {
        final ValueStack stack = new ValueStack();
        final FunctionTable functionTable = new FunctionTable();
        final IO io;

        Interpreter(Scanner in, PrintStream out) {
            io = new IO(in, out);
        }

        Instructions process(final List<String> lines) {
            System.err.println("=== INPUT");
            lines.forEach(System.err::println);
            final Tokens tokens = new Tokens(lines);
            final Instructions instructions = parse(tokens);
            System.err.println("=== OUTPUT");
            instructions.forEach(System.err::println);
            final Context context = new Context(stack, functionTable, io);
            instructions.forEach(i -> i.execute(context));
            return instructions;
        }

        Instructions parse(final Tokens tokens) {
            final Parser parser = new Parser(tokens);
            parser.beginBlock();
            while (tokens.hasNext()) {
                final String token = tokens.next();
                parser.parse(token);
            }
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            return instructions;
        }

    }

    public static void main(String args[]) {
        new Solution().run();
    }

    private final InputStream input;
    private final PrintStream output;

    Solution() {
        this(System.in, System.out);
    }

    Solution(InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void run() {
        final Scanner in = new Scanner(input);

        final int count = in.nextInt();
        in.nextLine(); // empty

        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String line = in.nextLine();
             lines.add(line);
        }
        final Interpreter interpreter = new Interpreter(in, output);
        interpreter.process(lines);
    }
}